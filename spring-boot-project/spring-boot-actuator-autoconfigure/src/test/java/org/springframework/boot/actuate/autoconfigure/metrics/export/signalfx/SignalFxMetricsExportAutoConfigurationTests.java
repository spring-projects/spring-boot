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

package org.springframework.boot.actuate.autoconfigure.metrics.export.signalfx;

import java.util.Map;

import io.micrometer.core.instrument.Clock;
import io.micrometer.signalfx.SignalFxConfig;
import io.micrometer.signalfx.SignalFxMeterRegistry;
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
 * Tests for {@link SignalFxMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class SignalFxMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(SignalFxMetricsExportAutoConfiguration.class));

	@Test
	public void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(SignalFxMeterRegistry.class));
	}

	@Test
	public void failsWithoutAnAccessToken() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	public void autoConfiguresWithAnAccessToken() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues(
						"management.metrics.export.signalfx.access-token=abcde")
				.run((context) -> assertThat(context)
						.hasSingleBean(SignalFxMeterRegistry.class)
						.hasSingleBean(Clock.class).hasSingleBean(SignalFxConfig.class));
	}

	@Test
	public void autoConfigurationCanBeDisabled() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
				.withPropertyValues(
						"management.metrics.export.signalfx.access-token=abcde",
						"management.metrics.export.signalfx.enabled=false")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(SignalFxMeterRegistry.class)
						.doesNotHaveBean(SignalFxConfig.class));
	}

	@Test
	public void allowsConfigToBeCustomized() {
		this.contextRunner
				.withPropertyValues(
						"management.metrics.export.signalfx.access-token=abcde")
				.withUserConfiguration(CustomConfigConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Clock.class)
						.hasSingleBean(SignalFxMeterRegistry.class)
						.hasSingleBean(SignalFxConfig.class).hasBean("customConfig"));
	}

	@Test
	public void allowsRegistryToBeCustomized() {
		this.contextRunner
				.withPropertyValues(
						"management.metrics.export.signalfx.access-token=abcde")
				.withUserConfiguration(CustomRegistryConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(Clock.class)
						.hasSingleBean(SignalFxConfig.class)
						.hasSingleBean(SignalFxMeterRegistry.class)
						.hasBean("customRegistry"));
	}

	@Test
	public void stopsMeterRegistryWhenContextIsClosed() {
		this.contextRunner
				.withPropertyValues(
						"management.metrics.export.signalfx.access-token=abcde")
				.withUserConfiguration(BaseConfiguration.class).run((context) -> {
					SignalFxMeterRegistry registry = spyOnDisposableBean(
							SignalFxMeterRegistry.class, context);
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
		public SignalFxConfig customConfig() {
			return new SignalFxConfig() {

				@Override
				public String get(String k) {
					if ("signalfx.accessToken".equals(k)) {
						return "abcde";
					}
					return null;
				}

			};
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean(destroyMethod = "stop")
		public SignalFxMeterRegistry customRegistry(SignalFxConfig config, Clock clock) {
			return new SignalFxMeterRegistry(config, clock);
		}

	}

}
