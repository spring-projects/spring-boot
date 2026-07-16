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

package org.springframework.boot.jms.autoconfigure.health;

import java.time.Duration;

import jakarta.jms.ConnectionFactory;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.jms.health.JmsHealthIndicator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JmsHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Venkata Naga Sai Srikanth Gollapudi
 */
class JmsHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JmsHealthContributorAutoConfiguration.class,
				HealthContributorAutoConfiguration.class))
		.withBean(ConnectionFactory.class, () -> mock(ConnectionFactory.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(JmsHealthIndicator.class));
	}

	@Test
	void runWhenStartTimeoutIsConfiguredShouldCreateIndicatorWithConfiguredStartTimeout() {
		this.contextRunner.withPropertyValues("management.health.jms.start-timeout=10ms").run((context) -> {
			assertThat(context).hasSingleBean(JmsHealthIndicator.class);
			assertThat(context).hasSingleBean(JmsHealthIndicatorProperties.class);
			assertThat(context.getBean(JmsHealthIndicatorProperties.class).getStartTimeout())
				.isEqualTo(Duration.ofMillis(10));
			assertThat(context.getBean(JmsHealthIndicator.class)).hasFieldOrPropertyWithValue("startTimeout",
					Duration.ofMillis(10));
		});
	}

	@Test
	void runWhenStartTimeoutIsZeroShouldFail() {
		this.contextRunner.withPropertyValues("management.health.jms.start-timeout=0ms")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessage("'startTimeout' must be greater than 0"));
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withPropertyValues("management.health.jms.enabled:false")
			.run((context) -> assertThat(context).doesNotHaveBean(JmsHealthIndicator.class));
	}

}
