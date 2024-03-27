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
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exemplars.ExemplarSampler;
import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;
import io.prometheus.client.exporter.BasicAuthHttpConnectionFactory;
import io.prometheus.client.exporter.DefaultHttpConnectionFactory;
import io.prometheus.client.exporter.HttpConnectionFactory;
import io.prometheus.client.exporter.PushGateway;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.DualPrometheusMetricsExportAutoConfigurationTests.CustomSecondEndpointConfiguration.SecondPrometheusScrapeEndpoint;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusPushGatewayManager;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusSimpleclientScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PrometheusSimpleclientMetricsExportAutoConfiguration} and
 * {@link PrometheusMetricsExportAutoConfiguration} with both Prometheus clients on the
 * classpath.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Jonatan Ivanov
 */
@SuppressWarnings({ "removal", "deprecation" })
@ExtendWith(OutputCaptureExtension.class)
class DualPrometheusMetricsExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(PrometheusSimpleclientMetricsExportAutoConfiguration.class,
				PrometheusMetricsExportAutoConfiguration.class));

	@Test
	void backsOffWithoutAClock() {
		this.contextRunner.run((context) -> assertThat(context)
			.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
			.doesNotHaveBean(io.micrometer.prometheus.PrometheusMeterRegistry.class));
	}

	@Test
	void autoConfiguresItsConfigPrometheusRegistryAndMeterRegistry() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(io.micrometer.prometheus.PrometheusMeterRegistry.class)
				.hasSingleBean(CollectorRegistry.class)
				.hasSingleBean(io.micrometer.prometheus.PrometheusConfig.class)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
				.hasSingleBean(PrometheusRegistry.class)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithDefaultsEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.defaults.metrics.export.enabled=false")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(io.micrometer.prometheus.PrometheusMeterRegistry.class)
				.doesNotHaveBean(CollectorRegistry.class)
				.doesNotHaveBean(io.micrometer.prometheus.PrometheusConfig.class)
				.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
				.doesNotHaveBean(PrometheusRegistry.class)
				.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusConfig.class));
	}

	@Test
	void autoConfigurationCanBeDisabledWithSpecificEnabledProperty() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.prometheus.metrics.export.enabled=false")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(io.micrometer.prometheus.PrometheusMeterRegistry.class)
				.doesNotHaveBean(CollectorRegistry.class)
				.doesNotHaveBean(io.micrometer.prometheus.PrometheusConfig.class)
				.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusMeterRegistry.class)
				.doesNotHaveBean(PrometheusRegistry.class)
				.doesNotHaveBean(io.micrometer.prometheusmetrics.PrometheusConfig.class));
	}

	@Test
	void allowsCustomConfigToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomConfigConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(io.micrometer.prometheus.PrometheusMeterRegistry.class)
				.hasSingleBean(CollectorRegistry.class)
				.hasSingleBean(io.micrometer.prometheus.PrometheusConfig.class)
				.hasBean("customConfig")
				.hasSingleBean(PrometheusRegistry.class)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusConfig.class)
				.hasBean("otherCustomConfig"));
	}

	@Test
	void allowsCustomRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomRegistryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(io.micrometer.prometheus.PrometheusMeterRegistry.class)
				.hasBean("customRegistry")
				.hasSingleBean(CollectorRegistry.class)
				.hasSingleBean(io.micrometer.prometheus.PrometheusConfig.class)
				.hasSingleBean(PrometheusRegistry.class)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusConfig.class)
				.hasBean("otherCustomRegistry"));
	}

	@Test
	void allowsCustomCollectorRegistryToBeUsed() {
		this.contextRunner.withUserConfiguration(CustomCollectorRegistryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(io.micrometer.prometheus.PrometheusMeterRegistry.class)
				.hasBean("customCollectorRegistry")
				.hasSingleBean(CollectorRegistry.class)
				.hasSingleBean(io.micrometer.prometheus.PrometheusConfig.class)
				.hasBean("customPrometheusRegistry")
				.hasSingleBean(PrometheusRegistry.class)
				.hasSingleBean(io.micrometer.prometheusmetrics.PrometheusConfig.class));
	}

	@Test
	void autoConfiguresExemplarSamplerIfSpanContextSupplierIsPresent() {
		this.contextRunner.withUserConfiguration(ExemplarsConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(SpanContextSupplier.class)
				.hasSingleBean(ExemplarSampler.class)
				.hasSingleBean(io.micrometer.prometheus.PrometheusMeterRegistry.class));
	}

	@Test
	void allowsCustomExemplarSamplerToBeUsed() {
		this.contextRunner.withUserConfiguration(ExemplarsConfiguration.class)
			.withBean("customExemplarSampler", ExemplarSampler.class, () -> mock(ExemplarSampler.class))
			.run((context) -> assertThat(context).hasSingleBean(ExemplarSampler.class)
				.getBean(ExemplarSampler.class)
				.isSameAs(context.getBean("customExemplarSampler")));
	}

	@Test
	void exemplarSamplerIsNotAutoConfiguredIfSpanContextSupplierIsMissing() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(SpanContextSupplier.class)
				.doesNotHaveBean(ExemplarSampler.class)
				.hasSingleBean(io.micrometer.prometheus.PrometheusMeterRegistry.class));
	}

	@Test
	void addsScrapeEndpointToManagementContext() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withUserConfiguration(BaseConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=prometheus")
			.run((context) -> assertThat(context).hasSingleBean(PrometheusScrapeEndpoint.class)
				.doesNotHaveBean(PrometheusSimpleclientScrapeEndpoint.class));
	}

	@Test
	void scrapeEndpointNotAddedToManagementContextWhenNotExposed() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(PrometheusSimpleclientScrapeEndpoint.class)
				.doesNotHaveBean(PrometheusScrapeEndpoint.class));
	}

	@Test
	void scrapeEndpointCanBeDisabled() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.include=prometheus")
			.withPropertyValues("management.endpoint.prometheus.enabled=false")
			.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(PrometheusSimpleclientScrapeEndpoint.class)
				.doesNotHaveBean(PrometheusScrapeEndpoint.class));
	}

	@Test
	void allowsCustomScrapeEndpointToBeUsed() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withUserConfiguration(CustomEndpointConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customEndpoint")
				.hasSingleBean(PrometheusSimpleclientScrapeEndpoint.class));
	}

	@Test
	void allowsCustomSecondScrapeEndpointToBeUsed() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withUserConfiguration(CustomSecondEndpointConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customSecondEndpoint")
				.hasSingleBean(PrometheusSimpleclientScrapeEndpoint.class)
				.hasSingleBean(SecondPrometheusScrapeEndpoint.class)
				.hasSingleBean(PrometheusScrapeEndpoint.class));
	}

	@Test
	void pushGatewayIsNotConfiguredWhenEnabledFlagIsNotSet() {
		this.contextRunner.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(PrometheusPushGatewayManager.class));
	}

	@Test
	void withPushGatewayEnabled(CapturedOutput output) {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withPropertyValues("management.prometheus.metrics.export.pushgateway.enabled=true")
			.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> {
				assertThat(output).doesNotContain("Invalid PushGateway base url");
				hasGatewayURL(context, "http://localhost:9091/metrics/");
			});
	}

	@Test
	void withPushGatewayNoBasicAuth() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withPropertyValues("management.prometheus.metrics.export.pushgateway.enabled=true")
			.withUserConfiguration(BaseConfiguration.class)
			.run(hasHttpConnectionFactory((httpConnectionFactory) -> assertThat(httpConnectionFactory)
				.isInstanceOf(DefaultHttpConnectionFactory.class)));
	}

	@Test
	void withCustomPushGatewayURL() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withPropertyValues("management.prometheus.metrics.export.pushgateway.enabled=true",
					"management.prometheus.metrics.export.pushgateway.base-url=https://example.com:8080")
			.withUserConfiguration(BaseConfiguration.class)
			.run((context) -> hasGatewayURL(context, "https://example.com:8080/metrics/"));
	}

	@Test
	void withPushGatewayBasicAuth() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class))
			.withPropertyValues("management.prometheus.metrics.export.pushgateway.enabled=true",
					"management.prometheus.metrics.export.pushgateway.username=admin",
					"management.prometheus.metrics.export.pushgateway.password=secret")
			.withUserConfiguration(BaseConfiguration.class)
			.run(hasHttpConnectionFactory((httpConnectionFactory) -> assertThat(httpConnectionFactory)
				.isInstanceOf(BasicAuthHttpConnectionFactory.class)));
	}

	private void hasGatewayURL(AssertableApplicationContext context, String url) {
		assertThat(getPushGateway(context)).hasFieldOrPropertyWithValue("gatewayBaseURL", url);
	}

	private ContextConsumer<AssertableApplicationContext> hasHttpConnectionFactory(
			ThrowingConsumer<HttpConnectionFactory> httpConnectionFactory) {
		return (context) -> {
			PushGateway pushGateway = getPushGateway(context);
			httpConnectionFactory
				.accept((HttpConnectionFactory) ReflectionTestUtils.getField(pushGateway, "connectionFactory"));
		};
	}

	private PushGateway getPushGateway(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(PrometheusPushGatewayManager.class);
		PrometheusPushGatewayManager gatewayManager = context.getBean(PrometheusPushGatewayManager.class);
		return (PushGateway) ReflectionTestUtils.getField(gatewayManager, "pushGateway");
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
		io.micrometer.prometheus.PrometheusConfig customConfig() {
			return (key) -> null;
		}

		@Bean
		io.micrometer.prometheusmetrics.PrometheusConfig otherCustomConfig() {
			return (key) -> null;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomRegistryConfiguration {

		@Bean
		io.micrometer.prometheus.PrometheusMeterRegistry customRegistry(
				io.micrometer.prometheus.PrometheusConfig config, CollectorRegistry collectorRegistry, Clock clock) {
			return new io.micrometer.prometheus.PrometheusMeterRegistry(config, collectorRegistry, clock);
		}

		@Bean
		io.micrometer.prometheusmetrics.PrometheusMeterRegistry otherCustomRegistry(
				io.micrometer.prometheusmetrics.PrometheusConfig config, PrometheusRegistry prometheusRegistry,
				Clock clock) {
			return new io.micrometer.prometheusmetrics.PrometheusMeterRegistry(config, prometheusRegistry, clock);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomCollectorRegistryConfiguration {

		@Bean
		CollectorRegistry customCollectorRegistry() {
			return new CollectorRegistry();
		}

		@Bean
		PrometheusRegistry customPrometheusRegistry() {
			return new PrometheusRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomEndpointConfiguration {

		@Bean
		PrometheusSimpleclientScrapeEndpoint customEndpoint(CollectorRegistry collectorRegistry) {
			return new PrometheusSimpleclientScrapeEndpoint(collectorRegistry);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class CustomSecondEndpointConfiguration {

		@Bean
		PrometheusScrapeEndpoint prometheusScrapeEndpoint(PrometheusRegistry prometheusRegistry) {
			return new PrometheusScrapeEndpoint(prometheusRegistry);
		}

		@Bean
		SecondPrometheusScrapeEndpoint customSecondEndpoint(CollectorRegistry collectorRegistry) {
			return new SecondPrometheusScrapeEndpoint(collectorRegistry);
		}

		@WebEndpoint(id = "prometheussc")
		static class SecondPrometheusScrapeEndpoint extends PrometheusSimpleclientScrapeEndpoint {

			SecondPrometheusScrapeEndpoint(CollectorRegistry collectorRegistry) {
				super(collectorRegistry);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseConfiguration.class)
	static class ExemplarsConfiguration {

		@Bean
		SpanContextSupplier spanContextSupplier() {
			return new SpanContextSupplier() {

				@Override
				public String getTraceId() {
					return null;
				}

				@Override
				public String getSpanId() {
					return null;
				}

				@Override
				public boolean isSampled() {
					return false;
				}

			};
		}

	}

}
