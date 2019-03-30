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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthIndicatorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class HealthIndicatorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(HealthIndicatorAutoConfiguration.class));

	@Test
	public void runWhenNoOtherIndicatorsShouldCreateDefaultApplicationHealthIndicator() {
		this.contextRunner
				.run((context) -> assertThat(context).getBean(HealthIndicator.class)
						.isInstanceOf(ApplicationHealthIndicator.class));
	}

	@Test
	public void runWhenHasDefinedIndicatorShouldNotCreateDefaultApplicationHealthIndicator() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicatorConfiguration.class)
				.run((context) -> assertThat(context).getBean(HealthIndicator.class)
						.isInstanceOf(CustomHealthIndicator.class));
	}

	@Test
	public void runWhenHasDefaultsDisabledAndNoSingleIndicatorEnabledShouldCreateDefaultApplicationHealthIndicator() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicatorConfiguration.class)
				.withPropertyValues("management.health.defaults.enabled:false")
				.run((context) -> assertThat(context).getBean(HealthIndicator.class)
						.isInstanceOf(ApplicationHealthIndicator.class));

	}

	@Test
	public void runWhenHasDefaultsDisabledAndSingleIndicatorEnabledShouldCreateEnabledIndicator() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicatorConfiguration.class)
				.withPropertyValues("management.health.defaults.enabled:false",
						"management.health.custom.enabled:true")
				.run((context) -> assertThat(context).getBean(HealthIndicator.class)
						.isInstanceOf(CustomHealthIndicator.class));

	}

	@Test
	public void runShouldCreateOrderedHealthAggregator() {
		this.contextRunner
				.run((context) -> assertThat(context).getBean(HealthAggregator.class)
						.isInstanceOf(OrderedHealthAggregator.class));
	}

	@Test
	public void runWhenHasCustomOrderPropertyShouldCreateOrderedHealthAggregator() {
		this.contextRunner.withPropertyValues("management.health.status.order:UP,DOWN")
				.run((context) -> {
					OrderedHealthAggregator aggregator = context
							.getBean(OrderedHealthAggregator.class);
					Map<String, Health> healths = new LinkedHashMap<>();
					healths.put("foo", Health.up().build());
					healths.put("bar", Health.down().build());
					Health aggregate = aggregator.aggregate(healths);
					assertThat(aggregate.getStatus()).isEqualTo(Status.UP);
				});
	}

	@Test
	public void runWhenHasCustomHealthAggregatorShouldNotCreateOrderedHealthAggregator() {
		this.contextRunner
				.withUserConfiguration(CustomHealthAggregatorConfiguration.class)
				.run((context) -> assertThat(context).getBean(HealthAggregator.class)
						.isNotInstanceOf(OrderedHealthAggregator.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnEnabledHealthIndicator("custom")
		public HealthIndicator customHealthIndicator() {
			return new CustomHealthIndicator();
		}

	}

	static class CustomHealthIndicator implements HealthIndicator {

		@Override
		public Health health() {
			return Health.down().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHealthAggregatorConfiguration {

		@Bean
		public HealthAggregator healthAggregator() {
			return (healths) -> Health.down().build();
		}

	}

}
