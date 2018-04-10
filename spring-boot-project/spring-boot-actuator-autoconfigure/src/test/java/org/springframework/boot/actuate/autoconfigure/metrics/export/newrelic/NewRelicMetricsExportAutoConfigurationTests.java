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

import java.util.Map;

import io.micrometer.core.instrument.Clock;
import io.micrometer.newrelic.NewRelicConfig;
import io.micrometer.newrelic.NewRelicMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 *
 * Tests for {@link NewRelicMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class NewRelicMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(NewRelicMetricsExportAutoConfiguration.class));

	@Test
	public void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(NewRelicMeterRegistry.class));
	}

	@Test
	public void failsWithoutAnApiKey() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	public void failsWithoutAnAccountId() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde")
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	public void autoConfiguresWithAccountIdAndApiKey() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context)
						.hasSingleBean(NewRelicMeterRegistry.class)
						.hasSingleBean(Clock.class).hasSingleBean(NewRelicConfig.class));
	}

	@Test
	public void autoConfigurationCanBeDisabled() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(NewRelicMeterRegistry.class)
						.doesNotHaveBean(NewRelicConfig.class));
	}

	@Test
	public void allowsConfigToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context).hasSingleBean(NewRelicConfig.class)
						.hasBean("customConfig"));
	}

	@Test
	public void allowsRegistryToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=12345")
				.run((context) -> assertThat(context)
						.hasSingleBean(NewRelicMeterRegistry.class)
						.hasBean("customRegistry"));
	}

	@Test
	public void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner
				.withPropertyValues("management.metrics.export.newrelic.api-key=abcde",
						"management.metrics.export.newrelic.account-id=abcde")
				.withUserConfiguration(BaseConfiguration.class).run((context) -> {
					NewRelicMeterRegistry registry = spyOnDisposableBean(
							NewRelicMeterRegistry.class, context);
					context.close();
					verify(registry).stop();
				});
	}

	@SuppressWarnings("unchecked")
	private <T> T spyOnDisposableBean(Class<T> type,
			AssertableApplicationContext context) {
		String[] names = context.getBeanNamesForType(type);
		assertThat(names).hasSize(1);
		String registryBeanName = names[0];
		Map<String, Object> disposableBeans = (Map<String, Object>) ReflectionTestUtils
				.getField(context.getAutowireCapableBeanFactory(), "disposableBeans");
		Object registryAdapter = disposableBeans.get(registryBeanName);
		T registry = (T) spy(ReflectionTestUtils.getField(registryAdapter, "bean"));
		ReflectionTestUtils.setField(registryAdapter, "bean", registry);
		return registry;
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public Clock customClock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
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
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		public NewRelicMeterRegistry customRegistry(NewRelicConfig config, Clock clock) {
			return new NewRelicMeterRegistry(config, clock);
		}

	}

}
