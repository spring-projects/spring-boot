/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.health.CompositeReactiveHealthIndicatorFactory;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthStatusHttpMapper;
import org.springframework.boot.actuate.health.HealthWebEndpointResponseMapper;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for health endpoint web extensions.
 *
 * @author Stephane Nicoll
 */
@Configuration
@EnableConfigurationProperties(HealthIndicatorProperties.class)
class HealthEndpointWebExtensionConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public HealthStatusHttpMapper createHealthStatusHttpMapper(HealthIndicatorProperties healthIndicatorProperties) {
		HealthStatusHttpMapper statusHttpMapper = new HealthStatusHttpMapper();
		if (healthIndicatorProperties.getHttpMapping() != null) {
			statusHttpMapper.addStatusMapping(healthIndicatorProperties.getHttpMapping());
		}
		return statusHttpMapper;
	}

	@Bean
	@ConditionalOnMissingBean
	public HealthWebEndpointResponseMapper healthWebEndpointResponseMapper(HealthStatusHttpMapper statusHttpMapper,
			HealthEndpointProperties properties) {
		return new HealthWebEndpointResponseMapper(statusHttpMapper, properties.getShowDetails(),
				properties.getRoles());
	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.REACTIVE)
	static class ReactiveWebHealthConfiguration {

		private final ReactiveHealthIndicator reactiveHealthIndicator;

		ReactiveWebHealthConfiguration(ObjectProvider<HealthAggregator> healthAggregator,
				ObjectProvider<Map<String, ReactiveHealthIndicator>> reactiveHealthIndicators,
				ObjectProvider<Map<String, HealthIndicator>> healthIndicators) {
			this.reactiveHealthIndicator = new CompositeReactiveHealthIndicatorFactory().createReactiveHealthIndicator(
					healthAggregator.getIfAvailable(OrderedHealthAggregator::new),
					reactiveHealthIndicators.getIfAvailable(Collections::emptyMap),
					healthIndicators.getIfAvailable(Collections::emptyMap));
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledEndpoint
		@ConditionalOnBean(HealthEndpoint.class)
		public ReactiveHealthEndpointWebExtension reactiveHealthEndpointWebExtension(
				HealthWebEndpointResponseMapper responseMapper) {
			return new ReactiveHealthEndpointWebExtension(this.reactiveHealthIndicator, responseMapper);
		}

	}

	@Configuration
	@ConditionalOnWebApplication(type = Type.SERVLET)
	static class ServletWebHealthConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnEnabledEndpoint
		@ConditionalOnBean(HealthEndpoint.class)
		public HealthEndpointWebExtension healthEndpointWebExtension(ApplicationContext applicationContext,
				HealthWebEndpointResponseMapper responseMapper) {
			return new HealthEndpointWebExtension(HealthIndicatorBeansComposite.get(applicationContext),
					responseMapper);
		}

	}

}
