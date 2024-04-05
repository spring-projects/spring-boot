/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus;

import io.micrometer.core.instrument.Clock;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.tracer.common.SpanContext;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PrometheusMetricsExportAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Jonatan Ivanov
 */
class PrometheusMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withClassLoader(new FilteredClassLoader("io.micrometer.prometheus.", "io.prometheus.client"))
		.withConfiguration(AutoConfigurations.of(PrometheusMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context)
			.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class));
	}

	@Test
	void autoConfiguresItsConfigCollectorRegistryAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
				.hasSingleBean(PrometheusRegistry.class)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
				.doesNotHaveBean(PrometheusRegistry.class)
				.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.prometheus.metrics.export.enabled=false")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
				.doesNotHaveBean(PrometheusRegistry.class)
				.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
				.hasSingleBean(PrometheusRegistry.class)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusConfig.class)
				.hasBean("customConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.run((context) -> assertThat(context)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
				.hasBean("customRegistry")
				.hasSingleBean(PrometheusRegistry.class)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusConfig.class));
	}

	@Test
	void allowsCustomCollectorRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomPrometheusRegistryConfiguration.class)
			.run((context) -> assertThat(context)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
				.hasBean("customPrometheusRegistry")
				.hasSingleBean(PrometheusRegistry.class)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusConfig.class));
	}

	@Test
	void autoConfiguresPrometheusMeterRegistryIfSpanContextIsPresent() {
		this.contextRunner.withUserConfiguration(ExemplarsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(SpanContext.class)
				.hasSingleBean(PrometheusMeterRegistry.class));
	}

	@Test
	void addsScrapeEndpointToManagementContext() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=prometheus")
			.run((context) -> assertThat(context).hasSingleBean(PrometheusScrapeEndpoint.class));
	}

	@Test
	void scrapeEndpointNotAddedToManagementContextWhenNotExposed() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(PrometheusScrapeEndpoint.class));
	}

	@Test
	void scrapeEndpointCanBeDisabled() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.include=prometheus")
			.withPropertyValues("management.endpoint.prometheus.enabled=false")
			.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(PrometheusScrapeEndpoint.class));
	}

	@Test
	void allowsCustomScrapeEndpointToBeUsed() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withUserConfiguration(CustomEndpointConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customEndpoint")
				.hasSingleBean(PrometheusScrapeEndpoint.class));
	}

	@Test
	void pushGatewayIsNotConfiguredWhenEnabledFlagIsNotSet() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(PrometheusPushGatewayManager.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		Clock clock() {
			return Clock.SYSTEM;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomConfigConfiguration {

		@Bean
		io.micrometer.prometheusmetrics.PrometheusConfig customConfig() {
			return (key) -> null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		io.micrometer.prometheusmetrics.PrometheusMeterRegistry customRegistry(
				io.micrometer.prometheusmetrics.PrometheusConfig config, PrometheusRegistry prometheusRegistry,
				Clock clock) {
			return new io.micrometer.prometheusmetrics.PrometheusMeterRegistry(config, prometheusRegistry, clock);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomPrometheusRegistryConfiguration {

		@Bean
		PrometheusRegistry customPrometheusRegistry() {
			return new PrometheusRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomEndpointConfiguration {

		@Bean
		PrometheusScrapeEndpoint customEndpoint(PrometheusRegistry prometheusRegistry) {
			return new PrometheusScrapeEndpoint(prometheusRegistry);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class ExemplarsConfiguration {

		@Bean
		SpanContext spanContext() {
			return new SpanContext() {

				@Override
				public String getCurrentTraceId() {
					return null;
				}

				@Override
				public String getCurrentSpanId() {
					return null;
				}

				@Override
				public boolean isCurrentSpanSampled() {
					return false;
				}

				@Override
				public void markCurrentSpanAsExemplar() {
				}
			};
		}

	}

}
