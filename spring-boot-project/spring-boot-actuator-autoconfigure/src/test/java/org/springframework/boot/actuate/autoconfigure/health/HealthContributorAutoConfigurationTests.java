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

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.PingHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class HealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class));

	@Test
	void runWhenNoOtherIndicatorsCreatesPingHealthIndicator() {
		this.contextRunner.run((context) -> assertThat(context).getBean(HealthIndicator.class)
			.isInstanceOf(PingHealthIndicator.class));
	}

	@Test
	void runWhenHasDefinedIndicatorCreatesPingHealthIndicator() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicatorConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(PingHealthIndicator.class)
				.hasSingleBean(CustomHealthIndicator.class));
	}

	@Test
	void runWhenHasDefaultsDisabledDoesNotCreatePingHealthIndicator() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicatorConfiguration.class)
			.withPropertyValues("management.health.defaults.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(HealthIndicator.class));

	}

	@Test
	void runWhenHasDefaultsDisabledAndPingIndicatorEnabledCreatesPingHealthIndicator() {
		this.contextRunner.withUserConfiguration(CustomHealthIndicatorConfiguration.class)
			.withPropertyValues("management.health.defaults.enabled:false", "management.health.ping.enabled:true")
			.run((context) -> assertThat(context).hasSingleBean(PingHealthIndicator.class));

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnEnabledHealthIndicator("custom")
		HealthIndicator customHealthIndicator() {
			return new CustomHealthIndicator();
		}

	}

	static class CustomHealthIndicator implements HealthIndicator {

		@Override
		public Health health() {
			return Health.down().build();
		}

	}

}
