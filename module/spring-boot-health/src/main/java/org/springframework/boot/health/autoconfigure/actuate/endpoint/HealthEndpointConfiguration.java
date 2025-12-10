/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import java.util.Set;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroupsPostProcessor;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.SimpleHttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.SimpleStatusAggregator;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

/**
 * Configuration for {@link HealthEndpoint} infrastructure beans.
 *
 * @author Phillip Webb
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
class HealthEndpointConfiguration {

	@Bean
	@ConditionalOnMissingBean
	StatusAggregator healthStatusAggregator(HealthEndpointProperties properties) {
		return new SimpleStatusAggregator(properties.getStatus().getOrder());
	}

	@Bean
	@ConditionalOnMissingBean
	HttpCodeStatusMapper healthHttpCodeStatusMapper(HealthEndpointProperties properties) {
		return new SimpleHttpCodeStatusMapper(properties.getStatus().getHttpMapping());
	}

	@Bean
	@ConditionalOnMissingBean(HealthEndpointGroups.class)
	AutoConfiguredHealthEndpointGroups healthEndpointGroups(ApplicationContext applicationContext,
			HealthEndpointProperties properties) {
		return new AutoConfiguredHealthEndpointGroups(applicationContext, properties);
	}

	@Bean
	GroupsHealthContributorNameValidator groupsHealthContributorNameValidator(
			ObjectProvider<HealthEndpointGroups> healthEndpointGroups) {
		return new GroupsHealthContributorNameValidator(healthEndpointGroups.getIfAvailable());
	}

	@Bean
	@ConditionalOnBooleanProperty(name = "management.endpoint.health.validate-group-membership", matchIfMissing = true)
	HealthEndpointGroupMembershipValidator healthEndpointGroupMembershipValidator(HealthEndpointProperties properties,
			HealthContributorRegistry healthContributorRegistry,
			ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry) {
		return new HealthEndpointGroupMembershipValidator(properties, healthContributorRegistry,
				reactiveHealthContributorRegistry.getIfAvailable());
	}

	@Bean
	@ConditionalOnMissingBean
	HealthEndpoint healthEndpoint(HealthContributorRegistry healthContributorRegistry,
			ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry,
			HealthEndpointGroups groups, HealthEndpointProperties properties) {
		return new HealthEndpoint(healthContributorRegistry, reactiveHealthContributorRegistry.getIfAvailable(), groups,
				properties.getLogging().getSlowIndicatorThreshold());
	}

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

		HealthEndpointGroupsBeanPostProcessor(ObjectProvider<HealthEndpointGroupsPostProcessor> postProcessors) {
			this.postProcessors = postProcessors;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof HealthEndpointGroups groups) {
				return applyPostProcessors(groups);
			}
			return bean;
		}

		private Object applyPostProcessors(HealthEndpointGroups bean) {
			for (HealthEndpointGroupsPostProcessor postProcessor : this.postProcessors.orderedStream()
				.toArray(HealthEndpointGroupsPostProcessor[]::new)) {
				bean = postProcessor.postProcessHealthEndpointGroups(bean);
			}
			return bean;
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

		@Nullable ReactiveHealthContributorRegistry fallbackRegistry;

		HealthEndpointGroupMembershipValidator(HealthEndpointProperties properties, HealthContributorRegistry registry,
				@Nullable ReactiveHealthContributorRegistry fallbackRegistry) {
			this.properties = properties;
			this.registry = registry;
			this.fallbackRegistry = fallbackRegistry;
		}

		@Override
		public void afterSingletonsInstantiated() {
			validateGroups();
		}

		private void validateGroups() {
			this.properties.getGroup().forEach((name, group) -> {
				validate(group.getInclude(), "Included", name);
				validate(group.getExclude(), "Excluded", name);
			});
		}

		private void validate(@Nullable Set<String> names, String type, String group) {
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

		private boolean contributorExists(String[] path) {
			return contributorExistsInMainRegistry(path) || contributorExistsInFallbackRegistry(path);
		}

		private boolean contributorExistsInMainRegistry(String[] path) {
			return contributorExists(path, this.registry, HealthContributors.class, HealthContributors::getContributor);
		}

		private boolean contributorExistsInFallbackRegistry(String[] path) {
			return contributorExists(path, this.fallbackRegistry, ReactiveHealthContributors.class,
					ReactiveHealthContributors::getContributor);
		}

		@SuppressWarnings("unchecked")
		private <C> boolean contributorExists(String[] path, @Nullable Object registry, Class<C> collectionType,
				BiFunction<C, String, Object> getFromCollection) {
			int pathOffset = 0;
			Object contributor = registry;
			while (pathOffset < path.length) {
				if (contributor == null || !collectionType.isInstance(contributor)) {
					return false;
				}
				contributor = getFromCollection.apply((C) contributor, path[pathOffset]);
				pathOffset++;
			}
			return (contributor != null);
		}

		/**
		 * Thrown when a contributor that does not exist is included in or excluded from a
		 * group.
		 */
		static class NoSuchHealthContributorException extends RuntimeException {

			NoSuchHealthContributorException(String type, String name, String group) {
				super(type + " health contributor '" + name + "' in group '" + group + "' does not exist");
			}

		}

	}

}
