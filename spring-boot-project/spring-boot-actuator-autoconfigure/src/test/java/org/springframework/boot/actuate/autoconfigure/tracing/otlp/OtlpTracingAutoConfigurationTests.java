/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing.otlp;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.internal.compression.GzipCompressor;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import okhttp3.HttpUrl;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.tracing.otlp.OtlpTracingConfigurations.ConnectionDetails.PropertiesOtlpTracingConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OtlpTracingAutoConfiguration}.
 *
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 * @author Eddú Meléndez
 */
class OtlpTracingAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OtlpTracingAutoConfiguration.class));

	private final ApplicationContextRunner tracingDisabledContextRunner = this.contextRunner
		.withPropertyValues("management.tracing.enabled=false");

	@Test
	void shouldNotSupplyBeansIfPropertyIsNotSet() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(OtlpHttpSpanExporter.class));
	}

	@Test
	void shouldNotSupplyBeansIfGrpcTransportIsEnabledButPropertyIsNotSet() {
		this.contextRunner.withPropertyValues("management.otlp.tracing.transport=grpc")
			.run((context) -> assertThat(context).doesNotHaveBean(OtlpGrpcSpanExporter.class));
	}

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4318/v1/traces")
			.run((context) -> assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class)
				.hasSingleBean(SpanExporter.class));
	}

	@Test
	void shouldCustomizeHttpTransportWithProperties() {
		this.contextRunner
			.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4317/v1/traces",
					"management.otlp.tracing.timeout=10m", "management.otlp.tracing.connect-timeout=20m",
					"management.otlp.tracing.compression=GZIP", "management.otlp.tracing.headers.spring=boot")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class).hasSingleBean(SpanExporter.class);
				OtlpHttpSpanExporter exporter = context.getBean(OtlpHttpSpanExporter.class);
				assertThat(exporter).extracting("delegate.httpSender.client")
					.hasFieldOrPropertyWithValue("connectTimeoutMillis", 1200000)
					.hasFieldOrPropertyWithValue("callTimeoutMillis", 600000);
				assertThat(exporter).extracting("delegate.httpSender.compressor").isInstanceOf(GzipCompressor.class);
				assertThat(exporter).extracting("delegate.httpSender.headerSupplier")
					.asInstanceOf(InstanceOfAssertFactories.type(Supplier.class))
					.satisfies((headerSupplier) -> assertThat(headerSupplier.get())
						.asInstanceOf(InstanceOfAssertFactories.map(String.class, List.class))
						.containsEntry("spring", List.of("boot")));
			});
	}

	@Test
	void shouldSupplyBeansIfGrpcTransportIsEnabled() {
		this.contextRunner
			.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4317/v1/traces",
					"management.otlp.tracing.transport=grpc")
			.run((context) -> assertThat(context).hasSingleBean(OtlpGrpcSpanExporter.class)
				.hasSingleBean(SpanExporter.class));
	}

	@Test
	void shouldCustomizeGrpcTransportWithProperties() {
		this.contextRunner
			.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4317/v1/traces",
					"management.otlp.tracing.transport=grpc", "management.otlp.tracing.timeout=10m",
					"management.otlp.tracing.connect-timeout=20m", "management.otlp.tracing.compression=GZIP",
					"management.otlp.tracing.headers.spring=boot")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpGrpcSpanExporter.class).hasSingleBean(SpanExporter.class);
				OtlpGrpcSpanExporter exporter = context.getBean(OtlpGrpcSpanExporter.class);
				assertThat(exporter).extracting("delegate.grpcSender.client")
					.hasFieldOrPropertyWithValue("connectTimeoutMillis", 1200000)
					.hasFieldOrPropertyWithValue("callTimeoutMillis", 600000);
				assertThat(exporter).extracting("delegate.grpcSender.compressor").isInstanceOf(GzipCompressor.class);
				assertThat(exporter).extracting("delegate.grpcSender.headersSupplier")
					.asInstanceOf(InstanceOfAssertFactories.type(Supplier.class))
					.satisfies((headerSupplier) -> assertThat(headerSupplier.get())
						.asInstanceOf(InstanceOfAssertFactories.map(String.class, List.class))
						.containsEntry("spring", List.of("boot")));
			});
	}

	@Test
	void shouldNotSupplyBeansIfGlobalTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(SpanExporter.class));
	}

	@Test
	void shouldNotSupplyBeansIfOtlpTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.otlp.tracing.export.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(SpanExporter.class));
	}

	@Test
	void shouldNotSupplyBeansIfTracingBridgeIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.micrometer.tracing"))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanExporter.class));
	}

	@Test
	void shouldNotSupplyBeansIfOtelSdkIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.sdk"))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanExporter.class));
	}

	@Test
	void shouldNotSupplyBeansIfOtelApiIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.api"))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanExporter.class));
	}

	@Test
	void shouldNotSupplyBeansIfExporterIsMissing() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.exporter"))
			.run((context) -> assertThat(context).doesNotHaveBean(SpanExporter.class));
	}

	@Test
	void shouldBackOffWhenCustomHttpExporterIsDefined() {
		this.contextRunner.withUserConfiguration(CustomHttpExporterConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customOtlpHttpSpanExporter")
				.hasSingleBean(SpanExporter.class));
	}

	@Test
	void shouldBackOffWhenCustomGrpcExporterIsDefined() {
		this.contextRunner.withUserConfiguration(CustomGrpcExporterConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customOtlpGrpcSpanExporter")
				.hasSingleBean(SpanExporter.class));
	}

	@Test
	void shouldNotSupplyOtlpHttpSpanExporterIfTracingIsDisabled() {
		this.tracingDisabledContextRunner
			.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4318/v1/traces")
			.run((context) -> assertThat(context).doesNotHaveBean(OtlpHttpSpanExporter.class));
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4318/v1/traces")
			.run((context) -> assertThat(context).hasSingleBean(PropertiesOtlpTracingConnectionDetails.class));
	}

	@Test
	void testConnectionFactoryWithOverridesWhenUsingCustomConnectionDetails() {
		this.contextRunner.withUserConfiguration(ConnectionDetailsConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(OtlpTracingConnectionDetails.class)
				.doesNotHaveBean(PropertiesOtlpTracingConnectionDetails.class);
			OtlpHttpSpanExporter otlpHttpSpanExporter = context.getBean(OtlpHttpSpanExporter.class);
			assertThat(otlpHttpSpanExporter).extracting("delegate.httpSender.url")
				.isEqualTo(HttpUrl.get("http://localhost:12345/v1/traces"));
		});
	}

	@Test
	void httpShouldUseMeterProviderIfSet() {
		this.contextRunner.withUserConfiguration(MeterProviderConfiguration.class)
			.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4318/v1/traces")
			.run((context) -> {
				OtlpHttpSpanExporter otlpHttpSpanExporter = context.getBean(OtlpHttpSpanExporter.class);
				assertThat(otlpHttpSpanExporter.toBuilder())
					.extracting("delegate.meterProviderSupplier", InstanceOfAssertFactories.type(Supplier.class))
					.satisfies((meterProviderSupplier) -> assertThat(meterProviderSupplier.get())
						.isSameAs(MeterProviderConfiguration.meterProvider));
			});
	}

	@Test
	void grpcShouldUseMeterProviderIfSet() {
		this.contextRunner.withUserConfiguration(MeterProviderConfiguration.class)
			.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4318/v1/traces",
					"management.otlp.tracing.transport=grpc")
			.run((context) -> {
				OtlpGrpcSpanExporter otlpGrpcSpanExporter = context.getBean(OtlpGrpcSpanExporter.class);
				assertThat(otlpGrpcSpanExporter.toBuilder())
					.extracting("delegate.meterProviderSupplier", InstanceOfAssertFactories.type(Supplier.class))
					.satisfies((meterProviderSupplier) -> assertThat(meterProviderSupplier.get())
						.isSameAs(MeterProviderConfiguration.meterProvider));
			});
	}

	@Test
	void shouldCustomizeHttpTransportWithOtlpHttpSpanExporterBuilderCustomizer() {
		Duration connectTimeout = Duration.ofMinutes(20);
		Duration timeout = Duration.ofMinutes(10);
		this.contextRunner
			.withBean("httpCustomizer1", OtlpHttpSpanExporterBuilderCustomizer.class,
					() -> (builder) -> builder.setConnectTimeout(connectTimeout))
			.withBean("httpCustomizer2", OtlpHttpSpanExporterBuilderCustomizer.class,
					() -> (builder) -> builder.setTimeout(timeout))
			.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4317/v1/traces")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class).hasSingleBean(SpanExporter.class);
				OtlpHttpSpanExporter exporter = context.getBean(OtlpHttpSpanExporter.class);
				assertThat(exporter).extracting("delegate.httpSender.client")
					.hasFieldOrPropertyWithValue("connectTimeoutMillis", (int) connectTimeout.toMillis())
					.hasFieldOrPropertyWithValue("callTimeoutMillis", (int) timeout.toMillis());
			});
	}

	@Test
	void shouldCustomizeGrpcTransportWhenEnabledWithOtlpGrpcSpanExporterBuilderCustomizer() {
		Duration timeout = Duration.ofMinutes(10);
		Duration connectTimeout = Duration.ofMinutes(20);
		this.contextRunner
			.withBean("grpcCustomizer1", OtlpGrpcSpanExporterBuilderCustomizer.class,
					() -> (builder) -> builder.setConnectTimeout(connectTimeout))
			.withBean("grpcCustomizer2", OtlpGrpcSpanExporterBuilderCustomizer.class,
					() -> (builder) -> builder.setTimeout(timeout))
			.withPropertyValues("management.otlp.tracing.endpoint=http://localhost:4317/v1/traces",
					"management.otlp.tracing.transport=grpc")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpGrpcSpanExporter.class).hasSingleBean(SpanExporter.class);
				OtlpGrpcSpanExporter exporter = context.getBean(OtlpGrpcSpanExporter.class);
				assertThat(exporter).extracting("delegate.grpcSender.client")
					.hasFieldOrPropertyWithValue("connectTimeoutMillis", (int) connectTimeout.toMillis())
					.hasFieldOrPropertyWithValue("callTimeoutMillis", (int) timeout.toMillis());
			});
	}

	@Configuration(proxyBeanMethods = false)
	private static final class MeterProviderConfiguration {

		static final MeterProvider meterProvider = (instrumentationScopeName) -> null;

		@Bean
		MeterProvider meterProvider() {
			return meterProvider;
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomHttpExporterConfiguration {

		@Bean
		OtlpHttpSpanExporter customOtlpHttpSpanExporter() {
			return OtlpHttpSpanExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomGrpcExporterConfiguration {

		@Bean
		OtlpGrpcSpanExporter customOtlpGrpcSpanExporter() {
			return OtlpGrpcSpanExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsConfiguration {

		@Bean
		OtlpTracingConnectionDetails otlpTracingConnectionDetails() {
			return (transport) -> "http://localhost:12345/v1/traces";
		}

	}

}
