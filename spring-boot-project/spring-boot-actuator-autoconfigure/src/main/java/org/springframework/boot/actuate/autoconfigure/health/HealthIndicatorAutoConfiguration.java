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

import java.util.Map;

import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistry;
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistryFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthIndicator}s.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Vedran Pavic
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ HealthIndicatorProperties.class })
public class HealthIndicatorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean({ HealthIndicator.class, ReactiveHealthIndicator.class })
	public ApplicationHealthIndicator applicationHealthIndicator() {
		return new ApplicationHealthIndicator();
	}

	@Bean
	@ConditionalOnMissingBean(HealthAggregator.class)
	public OrderedHealthAggregator healthAggregator(HealthIndicatorProperties properties) {
		OrderedHealthAggregator healthAggregator = new OrderedHealthAggregator();
		if (properties.getOrder() != null) {
			healthAggregator.setStatusOrder(properties.getOrder());
		}
		return healthAggregator;
	}

	@Bean
	@ConditionalOnMissingBean(HealthIndicatorRegistry.class)
	public HealthIndicatorRegistry healthIndicatorRegistry(ApplicationContext applicationContext) {
		return HealthIndicatorRegistryBeans.get(applicationContext);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Flux.class)
	static class ReactiveHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ReactiveHealthIndicatorRegistry reactiveHealthIndicatorRegistry(
				Map<String, ReactiveHealthIndicator> reactiveHealthIndicators,
				Map<String, HealthIndicator> healthIndicators) {
			return new ReactiveHealthIndicatorRegistryFactory()
					.createReactiveHealthIndicatorRegistry(reactiveHealthIndicators, healthIndicators);
		}

	}

}
