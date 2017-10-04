/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.health.CompositeHealthIndicatorFactory;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.StatusEndpoint;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthEndpoint}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
@Configuration
public class HealthEndpointAutoConfiguration {

	private final HealthIndicator healthIndicator;

	public HealthEndpointAutoConfiguration(ApplicationContext applicationContext,
			ObjectProvider<HealthAggregator> healthAggregator) {
		this.healthIndicator = getHealthIndicator(applicationContext,
				healthAggregator.getIfAvailable(OrderedHealthAggregator::new));
	}

	private HealthIndicator getHealthIndicator(ApplicationContext applicationContext,
			HealthAggregator healthAggregator) {
		Map<String, HealthIndicator> indicators = new LinkedHashMap<>();
		indicators.putAll(applicationContext.getBeansOfType(HealthIndicator.class));
		if (ClassUtils.isPresent("reactor.core.publisher.Flux", null)) {
			new ReactiveHealthIndicators().get(applicationContext)
					.forEach(indicators::putIfAbsent);
		}
		CompositeHealthIndicatorFactory factory = new CompositeHealthIndicatorFactory();
		return factory.createHealthIndicator(healthAggregator, indicators);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public HealthEndpoint healthEndpoint() {
		return new HealthEndpoint(this.healthIndicator);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint
	public StatusEndpoint statusEndpoint() {
		return new StatusEndpoint(this.healthIndicator);
	}

	private static class ReactiveHealthIndicators {

		public Map<String, HealthIndicator> get(ApplicationContext applicationContext) {
			Map<String, HealthIndicator> indicators = new LinkedHashMap<>();
			applicationContext.getBeansOfType(ReactiveHealthIndicator.class)
					.forEach((name, indicator) -> indicators.put(name, adapt(indicator)));
			return indicators;
		}

		private HealthIndicator adapt(ReactiveHealthIndicator indicator) {
			return () -> indicator.health().block();
		}

	}

}
