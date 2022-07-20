/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.SimpleHttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

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
	@ConditionalOnMissingBean
	HealthEndpointGroups healthEndpointGroups(ApplicationContext applicationContext,
			HealthEndpointProperties properties) {
		return new AutoConfiguredHealthEndpointGroups(applicationContext, properties);
	}

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

	@Bean
	@ConditionalOnMissingBean
	HealthEndpoint healthEndpoint(HealthContributorRegistry registry, HealthEndpointGroups groups,
			HealthEndpointProperties properties) {
		return new HealthEndpoint(registry, groups, properties.getLogging().getSlowIndicatorThreshold());
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
	 * Adapter to expose {@link ReactiveHealthContributor} beans as
	 * {@link HealthContributor} instances.
	 */
	private static class AdaptedReactiveHealthContributors {

		private final Map<String, HealthContributor> adapted;

		AdaptedReactiveHealthContributors(Map<String, ReactiveHealthContributor> reactiveContributors) {
			Map<String, HealthContributor> adapted = new LinkedHashMap<>();
			reactiveContributors.forEach((name, contributor) -> adapted.put(name, adapt(contributor)));
			this.adapted = Collections.unmodifiableMap(adapted);
		}

		private HealthContributor adapt(ReactiveHealthContributor contributor) {
			if (contributor instanceof ReactiveHealthIndicator healthIndicator) {
				return adapt(healthIndicator);
			}
			if (contributor instanceof CompositeReactiveHealthContributor healthContributor) {
				return adapt(healthContributor);
			}
			throw new IllegalStateException("Unsupported ReactiveHealthContributor type " + contributor.getClass());
		}

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

		private CompositeHealthContributor adapt(CompositeReactiveHealthContributor composite) {
			return new CompositeHealthContributor() {

				@Override
				public Iterator<NamedContributor<HealthContributor>> iterator() {
					Iterator<NamedContributor<ReactiveHealthContributor>> iterator = composite.iterator();
					return new Iterator<NamedContributor<HealthContributor>>() {

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

		Map<String, HealthContributor> get() {
			return this.adapted;
		}

	}

}
