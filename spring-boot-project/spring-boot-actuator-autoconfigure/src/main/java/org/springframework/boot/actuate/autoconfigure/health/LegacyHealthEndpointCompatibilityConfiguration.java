/*
 * Copyright 2012-2020 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

/**
 * Configuration to adapt legacy deprecated health endpoint classes and interfaces.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
@SuppressWarnings("deprecation")
@EnableConfigurationProperties(HealthIndicatorProperties.class)
class LegacyHealthEndpointCompatibilityConfiguration {

	@Bean
	@ConditionalOnMissingBean
	HealthAggregator healthAggregator(HealthIndicatorProperties healthIndicatorProperties) {
		OrderedHealthAggregator aggregator = new OrderedHealthAggregator();
		if (!CollectionUtils.isEmpty(healthIndicatorProperties.getOrder())) {
			aggregator.setStatusOrder(healthIndicatorProperties.getOrder());
		}
		return aggregator;
	}

	@Bean
	@ConditionalOnMissingBean(HealthIndicatorRegistry.class)
	HealthContributorRegistryHealthIndicatorRegistryAdapter healthIndicatorRegistry(
			HealthContributorRegistry healthContributorRegistry) {
		return new HealthContributorRegistryHealthIndicatorRegistryAdapter(healthContributorRegistry);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Mono.class)
	static class LegacyReactiveHealthEndpointCompatibilityConfiguration {

		@Bean
		@ConditionalOnMissingBean(ReactiveHealthIndicatorRegistry.class)
		ReactiveHealthContributorRegistryReactiveHealthIndicatorRegistryAdapter reactiveHealthIndicatorRegistry(
				ReactiveHealthContributorRegistry reactiveHealthContributorRegistry) {
			return new ReactiveHealthContributorRegistryReactiveHealthIndicatorRegistryAdapter(
					reactiveHealthContributorRegistry);
		}

	}

}
