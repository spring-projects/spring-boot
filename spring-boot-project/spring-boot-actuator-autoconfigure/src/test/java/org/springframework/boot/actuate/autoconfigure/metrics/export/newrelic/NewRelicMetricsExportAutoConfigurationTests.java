/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.newrelic;

import io.micrometer.core.instrument.Clock;
import io.micrometer.newrelic.NewRelicConfig;
import io.micrometer.newrelic.NewRelicMeterRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * Tests for {@link NewRelicMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class NewRelicMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(NewRelicMetricsExportAutoConfiguration.class,
							MetricsAutoConfiguration.class));

	@Test
	public void failsWithoutAnApiKey() {
		this.runner
				.withPropertyValues("management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	public void failsWithoutAnAccountId() {
		this.runner
				.withConfiguration(AutoConfigurations.of(
						NewRelicMetricsExportAutoConfiguration.class,
						MetricsAutoConfiguration.class))
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde")
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	public void autoConfiguresWithAccountIdAndApiKey() {
		this.runner
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context)
						.hasSingleBean(NewRelicMeterRegistry.class)
						.hasSingleBean(Clock.class).hasSingleBean(NewRelicConfig.class));
	}

	@Test
	public void autoConfigurationCanBeDisabled() {
		this.runner
				.withPropertyValues("management.metrics.export.newrelic.enabled=false",
						"management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(NewRelicMeterRegistry.class)
						.doesNotHaveBean(NewRelicConfig.class));
	}

	@Test
	public void allowsClockToBeCustomized() {
		this.runner.withUserConfiguration(CustomClockConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context).hasSingleBean(Clock.class)
						.hasBean("customClock"));
	}

	@Test
	public void allowsConfigToBeCustomized() {
		this.runner.withUserConfiguration(CustomConfigConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context).hasSingleBean(NewRelicConfig.class)
						.hasBean("customConfig"));
	}

	@Test
	public void allowsRegistryToBeCustomized() {
		this.runner.withUserConfiguration(CustomRegistryConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context)
						.hasSingleBean(NewRelicMeterRegistry.class)
						.hasBean("customRegistry"));
	}

	@Configuration
	static class CustomClockConfiguration {

		@Bean
		public Clock customClock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration
	static class CustomConfigConfiguration {

		@Bean
		public NewRelicConfig customConfig() {
			return new NewRelicConfig() {

				@Override
				public String get(String k) {
					if ("newrelic.accountId".equals(k)) {
						return "abcde";
					}
					if ("newrelic.apiKey".equals(k)) {
						return "12345";
					}
					return null;
				}

			};
		}

	}

	@Configuration
	static class CustomRegistryConfiguration {

		@Bean
		public NewRelicMeterRegistry customRegistry(NewRelicConfig config, Clock clock) {
			return new NewRelicMeterRegistry(config, clock);
		}

	}

}
