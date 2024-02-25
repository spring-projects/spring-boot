/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointGroupsPostProcessor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.NamedContributors;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.SimpleHttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Configuration for {@link HealthEndpoint} infrastructure beans.
 *
 * @author Phillip Webb
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
class HealthEndpointConfiguration {

	/**
     * Creates a new instance of the {@link StatusAggregator} interface if no other bean of the same type is present.
     * 
     * @param properties the {@link HealthEndpointProperties} object containing the health endpoint properties
     * @return a new instance of the {@link StatusAggregator} interface
     */
    @Bean
	@ConditionalOnMissingBean
	StatusAggregator healthStatusAggregator(HealthEndpointProperties properties) {
		return new SimpleStatusAggregator(properties.getStatus().getOrder());
	}

	/**
     * Creates a new instance of HttpCodeStatusMapper if no bean of this type is already present in the application context.
     * Uses the provided HealthEndpointProperties to initialize the SimpleHttpCodeStatusMapper with the HTTP mapping defined in the properties.
     * 
     * @param properties the HealthEndpointProperties used to initialize the SimpleHttpCodeStatusMapper
     * @return a new instance of SimpleHttpCodeStatusMapper if no bean of this type is already present, otherwise returns the existing bean
     */
    @Bean
	@ConditionalOnMissingBean
	HttpCodeStatusMapper healthHttpCodeStatusMapper(HealthEndpointProperties properties) {
		return new SimpleHttpCodeStatusMapper(properties.getStatus().getHttpMapping());
	}

	/**
     * Creates and returns an instance of {@link HealthEndpointGroups} by auto-configuring it with the provided
     * {@link ApplicationContext} and {@link HealthEndpointProperties}.
     * This method is annotated with {@link Bean} and {@link ConditionalOnMissingBean} to ensure that it is only
     * executed if there is no existing bean of type {@link HealthEndpointGroups} in the application context.
     *
     * @param applicationContext The {@link ApplicationContext} used for auto-configuration.
     * @param properties The {@link HealthEndpointProperties} used for auto-configuration.
     * @return An instance of {@link HealthEndpointGroups} that is auto-configured with the provided parameters.
     */
    @Bean
	@ConditionalOnMissingBean
	HealthEndpointGroups healthEndpointGroups(ApplicationContext applicationContext,
			HealthEndpointProperties properties) {
		return new AutoConfiguredHealthEndpointGroups(applicationContext, properties);
	}

	/**
     * Creates a HealthContributorRegistry bean if it is missing in the application context.
     * The registry is populated with health contributors based on the availability of reactive health contributors.
     * If the Flux class is present in the application classloader, the reactive health contributors are adapted and added to the registry.
     * Finally, an AutoConfiguredHealthContributorRegistry is created with the populated health contributors and group names.
     *
     * @param applicationContext         the application context
     * @param groups                      the health endpoint groups
     * @param healthContributors          the map of health contributors
     * @param reactiveHealthContributors  the map of reactive health contributors
     * @return the created HealthContributorRegistry bean
     */
    @Bean
	@ConditionalOnMissingBean
	HealthContributorRegistry healthContributorRegistry(ApplicationContext applicationContext,
			HealthEndpointGroups groups, Map<String, HealthContributor> healthContributors,
			Map<String, ReactiveHealthContributor> reactiveHealthContributors) {
		if (ClassUtils.isPresent("reactor.core.publisher.Flux", applicationContext.getClassLoader())) {
			healthContributors.putAll(new AdaptedReactiveHealthContributors(reactiveHealthContributors).get());
		}
		return new AutoConfiguredHealthContributorRegistry(healthContributors, groups.getNames());
	}

	/**
     * Creates a {@link HealthEndpointGroupMembershipValidator} bean if the property
     * "management.endpoint.health.validate-group-membership" is set to true or is missing.
     * 
     * @param properties the {@link HealthEndpointProperties} object containing the health endpoint properties
     * @param healthContributorRegistry the {@link HealthContributorRegistry} object containing the health contributor registry
     * @return the {@link HealthEndpointGroupMembershipValidator} bean
     */
    @Bean
	@ConditionalOnProperty(name = "management.endpoint.health.validate-group-membership", havingValue = "true",
			matchIfMissing = true)
	HealthEndpointGroupMembershipValidator healthEndpointGroupMembershipValidator(HealthEndpointProperties properties,
			HealthContributorRegistry healthContributorRegistry) {
		return new HealthEndpointGroupMembershipValidator(properties, healthContributorRegistry);
	}

	/**
     * Creates a new instance of the {@link HealthEndpoint} class.
     * 
     * @param registry the {@link HealthContributorRegistry} used to retrieve health contributors
     * @param groups the {@link HealthEndpointGroups} used to group health contributors
     * @param properties the {@link HealthEndpointProperties} used to configure the health endpoint
     * @return a new instance of the {@link HealthEndpoint} class
     */
    @Bean
	@ConditionalOnMissingBean
	HealthEndpoint healthEndpoint(HealthContributorRegistry registry, HealthEndpointGroups groups,
			HealthEndpointProperties properties) {
		return new HealthEndpoint(registry, groups, properties.getLogging().getSlowIndicatorThreshold());
	}

	/**
     * Creates a new instance of {@link HealthEndpointGroupsBeanPostProcessor} with the provided {@link HealthEndpointGroupsPostProcessor}.
     * 
     * @param healthEndpointGroupsPostProcessors the {@link HealthEndpointGroupsPostProcessor} to be used by the {@link HealthEndpointGroupsBeanPostProcessor}
     * @return a new instance of {@link HealthEndpointGroupsBeanPostProcessor}
     */
    @Bean
	static HealthEndpointGroupsBeanPostProcessor healthEndpointGroupsBeanPostProcessor(
			ObjectProvider<HealthEndpointGroupsPostProcessor> healthEndpointGroupsPostProcessors) {
		return new HealthEndpointGroupsBeanPostProcessor(healthEndpointGroupsPostProcessors);
	}

	/**
	 * {@link BeanPostProcessor} to invoke {@link HealthEndpointGroupsPostProcessor}
	 * beans.
	 */
	static class HealthEndpointGroupsBeanPostProcessor implements BeanPostProcessor {

		private final ObjectProvider<HealthEndpointGroupsPostProcessor> postProcessors;

		/**
         * Constructs a new HealthEndpointGroupsBeanPostProcessor with the specified postProcessors.
         * 
         * @param postProcessors the ObjectProvider of HealthEndpointGroupsPostProcessor instances to be used for post-processing
         */
        HealthEndpointGroupsBeanPostProcessor(ObjectProvider<HealthEndpointGroupsPostProcessor> postProcessors) {
			this.postProcessors = postProcessors;
		}

		/**
         * Apply post-processing logic after initialization of a bean.
         * 
         * @param bean the initialized bean object
         * @param beanName the name of the bean
         * @return the processed bean object
         * @throws BeansException if an error occurs during post-processing
         */
        @Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof HealthEndpointGroups groups) {
				return applyPostProcessors(groups);
			}
			return bean;
		}

		/**
         * Applies the post processors to the given HealthEndpointGroups bean.
         * 
         * @param bean the HealthEndpointGroups bean to be processed
         * @return the processed HealthEndpointGroups bean
         */
        private Object applyPostProcessors(HealthEndpointGroups bean) {
			for (HealthEndpointGroupsPostProcessor postProcessor : this.postProcessors.orderedStream()
				.toArray(HealthEndpointGroupsPostProcessor[]::new)) {
				bean = postProcessor.postProcessHealthEndpointGroups(bean);
			}
			return bean;
		}

	}

	/**
	 * Adapter to expose {@link ReactiveHealthContributor} beans as
	 * {@link HealthContributor} instances.
	 */
	private static class AdaptedReactiveHealthContributors {

		private final Map<String, HealthContributor> adapted;

		/**
         * Adapts the given map of ReactiveHealthContributors to a map of HealthContributors.
         * 
         * @param reactiveContributors the map of ReactiveHealthContributors to be adapted
         */
        AdaptedReactiveHealthContributors(Map<String, ReactiveHealthContributor> reactiveContributors) {
			Map<String, HealthContributor> adapted = new LinkedHashMap<>();
			reactiveContributors.forEach((name, contributor) -> adapted.put(name, adapt(contributor)));
			this.adapted = Collections.unmodifiableMap(adapted);
		}

		/**
         * Adapts a ReactiveHealthContributor to a HealthContributor.
         * 
         * @param contributor the ReactiveHealthContributor to adapt
         * @return the adapted HealthContributor
         * @throws IllegalStateException if the ReactiveHealthContributor type is unsupported
         */
        private HealthContributor adapt(ReactiveHealthContributor contributor) {
			if (contributor instanceof ReactiveHealthIndicator healthIndicator) {
				return adapt(healthIndicator);
			}
			if (contributor instanceof CompositeReactiveHealthContributor healthContributor) {
				return adapt(healthContributor);
			}
			throw new IllegalStateException("Unsupported ReactiveHealthContributor type " + contributor.getClass());
		}

		/**
         * Adapts a ReactiveHealthIndicator to a HealthIndicator.
         * 
         * @param indicator the ReactiveHealthIndicator to adapt
         * @return the adapted HealthIndicator
         */
        private HealthIndicator adapt(ReactiveHealthIndicator indicator) {
			return new HealthIndicator() {

				@Override
				public Health getHealth(boolean includeDetails) {
					return indicator.getHealth(includeDetails).block();
				}

				@Override
				public Health health() {
					return indicator.health().block();
				}

			};
		}

		/**
         * Adapts a CompositeReactiveHealthContributor to a CompositeHealthContributor.
         * 
         * @param composite the CompositeReactiveHealthContributor to be adapted
         * @return the adapted CompositeHealthContributor
         */
        private CompositeHealthContributor adapt(CompositeReactiveHealthContributor composite) {
			return new CompositeHealthContributor() {

				@Override
				public Iterator<NamedContributor<HealthContributor>> iterator() {
					Iterator<NamedContributor<ReactiveHealthContributor>> iterator = composite.iterator();
					return new Iterator<>() {

						@Override
						public boolean hasNext() {
							return iterator.hasNext();
						}

						@Override
						public NamedContributor<HealthContributor> next() {
							NamedContributor<ReactiveHealthContributor> next = iterator.next();
							return NamedContributor.of(next.getName(), adapt(next.getContributor()));
						}

					};
				}

				@Override
				public HealthContributor getContributor(String name) {
					return adapt(composite.getContributor(name));
				}

			};
		}

		/**
         * Returns the map of health contributors.
         *
         * @return the map of health contributors
         */
        Map<String, HealthContributor> get() {
			return this.adapted;
		}

	}

	/**
	 * {@link SmartInitializingSingleton} that validates health endpoint group membership,
	 * throwing a {@link NoSuchHealthContributorException} if an included or excluded
	 * contributor does not exist.
	 */
	static class HealthEndpointGroupMembershipValidator implements SmartInitializingSingleton {

		private final HealthEndpointProperties properties;

		private final HealthContributorRegistry registry;

		/**
         * Constructs a new HealthEndpointGroupMembershipValidator with the specified properties and registry.
         * 
         * @param properties the properties for the health endpoint
         * @param registry the registry for health contributors
         */
        HealthEndpointGroupMembershipValidator(HealthEndpointProperties properties,
				HealthContributorRegistry registry) {
			this.properties = properties;
			this.registry = registry;
		}

		/**
         * This method is called after all singleton beans have been instantiated.
         * It is responsible for validating the groups in the HealthEndpointGroupMembershipValidator class.
         */
        @Override
		public void afterSingletonsInstantiated() {
			validateGroups();
		}

		/**
         * Validates the groups by checking the included and excluded members.
         * 
         * @param None
         * @return None
         */
        private void validateGroups() {
			this.properties.getGroup().forEach((name, group) -> {
				validate(group.getInclude(), "Included", name);
				validate(group.getExclude(), "Excluded", name);
			});
		}

		/**
         * Validates the given set of names for a specific type and group.
         * 
         * @param names the set of names to validate
         * @param type the type of health contributor
         * @param group the group of health contributors
         * @throws NoSuchHealthContributorException if a health contributor does not exist
         */
        private void validate(Set<String> names, String type, String group) {
			if (CollectionUtils.isEmpty(names)) {
				return;
			}
			for (String name : names) {
				if ("*".equals(name)) {
					return;
				}
				String[] path = name.split("/");
				if (!contributorExists(path)) {
					throw new NoSuchHealthContributorException(type, name, group);
				}
			}
		}

		/**
         * Checks if a contributor exists in the registry based on the given path.
         * 
         * @param path The path to the contributor in the registry.
         * @return true if the contributor exists, false otherwise.
         */
        private boolean contributorExists(String[] path) {
			int pathOffset = 0;
			Object contributor = this.registry;
			while (pathOffset < path.length) {
				if (!(contributor instanceof NamedContributors)) {
					return false;
				}
				contributor = ((NamedContributors<?>) contributor).getContributor(path[pathOffset]);
				pathOffset++;
			}
			return (contributor != null);
		}

		/**
		 * Thrown when a contributor that does not exist is included in or excluded from a
		 * group.
		 */
		static class NoSuchHealthContributorException extends RuntimeException {

			/**
             * Constructs a new NoSuchHealthContributorException with the specified type, name, and group.
             * 
             * @param type the type of the health contributor
             * @param name the name of the health contributor
             * @param group the group of the health contributor
             */
            NoSuchHealthContributorException(String type, String name, String group) {
				super(type + " health contributor '" + name + "' in group '" + group + "' does not exist");
			}

		}

	}

}
