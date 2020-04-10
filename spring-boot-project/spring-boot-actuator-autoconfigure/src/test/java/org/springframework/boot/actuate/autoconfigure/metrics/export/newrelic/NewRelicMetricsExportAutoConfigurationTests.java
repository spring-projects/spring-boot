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

package org.springframework.boot.actuate.autoconfigure.metrics.export.newrelic;

import io.micrometer.core.instrument.Clock;
import io.micrometer.newrelic.NewRelicClientProvider;
import io.micrometer.newrelic.NewRelicConfig;
import io.micrometer.newrelic.NewRelicMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 *
 * Tests for {@link NewRelicMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class NewRelicMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(NewRelicMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(NewRelicMeterRegistry.class));
	}

	@Test
	void failsWithoutAnApiKey() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void failsWithoutAnAccountId() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde")
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void failsToAutoConfigureWithoutEventType() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345",
						"management.metrics.export.newrelic.event-type=")
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void autoConfiguresWithEventTypeOverridden() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345",
						"management.metrics.export.newrelic.event-type=wxyz")
				.run((context) -> assertThat(context).hasSingleBean(NewRelicMeterRegistry.class)
						.hasSingleBean(Clock.class).hasSingleBean(NewRelicConfig.class));
	}

	@Test
	void autoConfiguresWithMeterNameEventTypeEnabledAndWithoutEventType() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345",
						"management.metrics.export.newrelic.event-type=",
						"management.metrics.export.newrelic.meter-name-event-type-enabled=true")
				.run((context) -> assertThat(context).hasSingleBean(NewRelicMeterRegistry.class)
						.hasSingleBean(Clock.class).hasSingleBean(NewRelicConfig.class));
	}

	@Test
	void autoConfiguresWithAccountIdAndApiKey() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context).hasSingleBean(NewRelicMeterRegistry.class)
						.hasSingleBean(Clock.class).hasSingleBean(NewRelicConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabled() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.enabled=false")
				.run((context) -> assertThat(context).doesNotHaveBean(NewRelicMeterRegistry.class)
						.doesNotHaveBean(NewRelicConfig.class));
	}

	@Test
	void allowsConfigToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context).hasSingleBean(NewRelicConfig.class).hasBean("customConfig"));
	}

	@Test
	void allowsRegistryToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context).hasSingleBean(NewRelicMeterRegistry.class)
						.hasBean("customRegistry"));
	}

	@Test
	void allowsClientProviderToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomClientProviderConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> {
					assertThat(context).hasSingleBean(NewRelicMeterRegistry.class);
					assertThat(context.getBean(NewRelicMeterRegistry.class))
							.hasFieldOrPropertyWithValue("clientProvider", context.getBean("customClientProvider"));
				});
	}

	@Test
	void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=abcde")
				.withUserConfiguration(BaseConfiguration.class).run((context) -> {
					NewRelicMeterRegistry registry = context.getBean(NewRelicMeterRegistry.class);
					assertThat(registry.isClosed()).isFalse();
					context.close();
					assertThat(registry.isClosed()).isTrue();
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		Clock customClock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomConfigConfiguration {

		@Bean
		NewRelicConfig customConfig() {
			return (key) -> {
				if ("newrelic.accountId".equals(key)) {
					return "abcde";
				}
				if ("newrelic.apiKey".equals(key)) {
					return "12345";
				}
				return null;
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		NewRelicMeterRegistry customRegistry(NewRelicConfig config, Clock clock) {
			return new NewRelicMeterRegistry(config, clock);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomClientProviderConfiguration {

		@Bean
		NewRelicClientProvider customClientProvider() {
			return mock(NewRelicClientProvider.class);
		}

	}

}
