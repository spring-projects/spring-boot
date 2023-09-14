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

package org.springframework.boot.actuate.autoconfigure.metrics.export.otlp;

import io.micrometer.core.instrument.Clock;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.export.otlp.OtlpMetricsExportAutoConfiguration.PropertiesOtlpMetricsConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OtlpMetricsExportAutoConfiguration}.
 *
 * @author Eddú Meléndez
 */
class OtlpMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OtlpMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class));
	}

	@Test
	void autoConfiguresConfigAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(OtlpMeterRegistry.class)
				.hasSingleBean(OtlpConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class)
				.doesNotHaveBean(OtlpConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.otlp.metrics.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class)
				.doesNotHaveBean(OtlpConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(OtlpMeterRegistry.class)
				.hasSingleBean(OtlpConfig.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsRegistryToBeCustomized() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(OtlpMeterRegistry.class)
				.hasSingleBean(OtlpConfig.class)
				.hasBean("customRegistry"));
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(PropertiesOtlpMetricsConnectionDetails.class));
	}

	@Test
	void testConnectionFactoryWithOverridesWhenUsingCustomConnectionDetails() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class, ConnectionDetailsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class)
					.doesNotHaveBean(PropertiesOtlpMetricsConnectionDetails.class);
				OtlpConfig config = context.getBean(OtlpConfig.class);
				assertThat(config.url()).isEqualTo("http://localhost:12345/v1/metrics");
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
		OtlpConfig customConfig() {
			return (key) -> null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		OtlpMeterRegistry customRegistry(OtlpConfig config, Clock clock) {
			return new OtlpMeterRegistry(config, clock);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsConfiguration {

		@Bean
		OtlpMetricsConnectionDetails otlpConnectionDetails() {
			return () -> "http://localhost:12345/v1/metrics";
		}

	}

}
