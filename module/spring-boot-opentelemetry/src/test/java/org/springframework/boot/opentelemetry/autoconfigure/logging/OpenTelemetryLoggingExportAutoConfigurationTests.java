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

package org.springframework.boot.opentelemetry.autoconfigure.logging;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import okhttp3.HttpUrl;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.SdkLoggerProviderBuilderCustomizer;
import org.springframework.boot.opentelemetry.autoconfigure.logging.OpenTelemetryLoggingConnectionDetailsConfiguration.PropertiesOpenTelemetryLoggingConnectionDetails;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetryLoggingExportAutoConfiguration}.
 *
 * @author Toshiaki Maki
 * @author Moritz Halbritter
 */
class OpenTelemetryLoggingExportAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner;

	OpenTelemetryLoggingExportAutoConfigurationTests() {
		this.contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
			.of(OpenTelemetrySdkAutoConfiguration.class, OpenTelemetryLoggingExportAutoConfiguration.class));
	}

	@Test
	void registeredInAutoConfigurationImports() {
		assertThat(ImportCandidates.load(AutoConfiguration.class, null).getCandidates())
			.contains(OpenTelemetryLoggingExportAutoConfiguration.class.getName());
	}

	@ParameterizedTest
	@ValueSource(strings = { "io.opentelemetry.sdk.logs", "io.opentelemetry.api",
			"io.opentelemetry.exporter.otlp.http.logs" })
	void whenOpenTelemetryIsNotOnClasspathDoesNotProvideBeans(String packageName) {
		this.contextRunner.withClassLoader(new FilteredClassLoader(packageName)).run((context) -> {
			assertThat(context).doesNotHaveBean(OpenTelemetryLoggingConnectionDetails.class);
			assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
		});
	}

	@Test
	void whenHasEndpointPropertyProvidesBeans() {
		this.contextRunner
			.withPropertyValues("management.opentelemetry.logging.export.endpoint=http://localhost:4318/v1/logs")
			.run((context) -> {
				assertThat(context).hasSingleBean(OpenTelemetryLoggingConnectionDetails.class);
				OpenTelemetryLoggingConnectionDetails connectionDetails = context
					.getBean(OpenTelemetryLoggingConnectionDetails.class);
				assertThat(connectionDetails.getUrl(Transport.HTTP)).isEqualTo("http://localhost:4318/v1/logs");
				assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
				assertThat(context).hasSingleBean(LogRecordExporter.class);
			});
	}

	@Test
	void whenHasNoEndpointPropertyDoesNotProvideBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(OpenTelemetryLoggingConnectionDetails.class);
			assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
		});
	}

	@Test
	void whenOpenTelemetryLoggingExportEnabledPropertyIsFalseProvidesExpectedBeans() {
		this.contextRunner
			.withPropertyValues("management.opentelemetry.logging.export.enabled=false",
					"management.opentelemetry.logging.export.endpoint=http://localhost:4318/v1/logs")
			.run((context) -> {
				assertThat(context).doesNotHaveBean(OpenTelemetryLoggingConnectionDetails.class);
				assertThat(context).doesNotHaveBean(LogRecordExporter.class);
			});
	}

	@Test
	void whenLoggingExportEnabledPropertyIsFalseNoProvideExpectedBeans() {
		this.contextRunner
			.withPropertyValues("management.logging.export.enabled=false",
					"management.opentelemetry.logging.export.endpoint=http://localhost:4318/v1/logs")
			.run((context) -> {
				assertThat(context).doesNotHaveBean(OpenTelemetryLoggingConnectionDetails.class);
				assertThat(context).doesNotHaveBean(LogRecordExporter.class);
			});
	}

	@Test
	void whenHasCustomHttpExporterDoesNotProvideExporterBean() {
		this.contextRunner.withUserConfiguration(CustomHttpExporterConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customOtlpHttpLogRecordExporter")
				.hasSingleBean(LogRecordExporter.class));
	}

	@Test
	void whenHasCustomGrpcExporterDoesNotProvideExporterBean() {
		this.contextRunner.withUserConfiguration(CustomGrpcExporterConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customOtlpGrpcLogRecordExporter")
				.hasSingleBean(LogRecordExporter.class));
	}
	// FIXME

	@Test
	void whenHasCustomLoggingConnectionDetailsDoesNotProvideExporterBean() {
		this.contextRunner.withUserConfiguration(CustomOtlpLoggingConnectionDetailsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(OpenTelemetryLoggingConnectionDetails.class)
					.doesNotHaveBean(PropertiesOpenTelemetryLoggingConnectionDetails.class);
				OtlpHttpLogRecordExporter otlpHttpLogRecordExporter = context.getBean(OtlpHttpLogRecordExporter.class);
				assertThat(otlpHttpLogRecordExporter).extracting("delegate.httpSender.url")
					.isEqualTo(HttpUrl.get("https://otel.example.com/v1/logs"));
			});
	}

	@Test
	void whenHasNoTransportPropertySetUsesHttpExporter() {
		this.contextRunner
			.withPropertyValues("management.opentelemetry.logging.export.endpoint=http://localhost:4318/v1/logs")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
				assertThat(context).hasSingleBean(LogRecordExporter.class);
				assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
			});
	}

	@Test
	void whenHasTransportPropertySetToHttpUsesHttpExporter() {
		this.contextRunner
			.withPropertyValues("management.opentelemetry.logging.export.endpoint=http://localhost:4318/v1/logs",
					"management.opentelemetry.logging.export.transport=http")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
				assertThat(context).hasSingleBean(LogRecordExporter.class);
				assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
			});
	}

	@Test
	void whenHasTransportPropertySetToGrpcUsesGrpcExporter() {
		this.contextRunner
			.withPropertyValues("management.opentelemetry.logging.export.endpoint=http://localhost:4318/v1/logs",
					"management.opentelemetry.logging.export.transport=grpc")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpGrpcLogRecordExporter.class);
				assertThat(context).hasSingleBean(LogRecordExporter.class);
				assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
			});
	}

	@Test
	void whenHasMeterProviderBeanAddsItToHttpExporter() {
		this.contextRunner.withUserConfiguration(MeterProviderConfiguration.class)
			.withPropertyValues("management.opentelemetry.logging.export.endpoint=http://localhost:4318/v1/logs")
			.run((context) -> {
				OtlpHttpLogRecordExporter otlpHttpLogRecordExporter = context.getBean(OtlpHttpLogRecordExporter.class);
				assertThat(otlpHttpLogRecordExporter.toBuilder())
					.extracting("delegate.meterProviderSupplier", InstanceOfAssertFactories.type(Supplier.class))
					.satisfies((meterProviderSupplier) -> assertThat(meterProviderSupplier.get())
						.isSameAs(MeterProviderConfiguration.meterProvider));
			});
	}

	@Test
	void whenHasMeterProviderBeanAddsItToGrpcExporter() {
		this.contextRunner.withUserConfiguration(MeterProviderConfiguration.class)
			.withPropertyValues("management.opentelemetry.logging.export.endpoint=http://localhost:4318/v1/logs",
					"management.opentelemetry.logging.export.transport=grpc")
			.run((context) -> {
				OtlpGrpcLogRecordExporter otlpGrpcLogRecordExporter = context.getBean(OtlpGrpcLogRecordExporter.class);
				assertThat(otlpGrpcLogRecordExporter.toBuilder())
					.extracting("delegate.meterProviderSupplier", InstanceOfAssertFactories.type(Supplier.class))
					.satisfies((meterProviderSupplier) -> assertThat(meterProviderSupplier.get())
						.isSameAs(MeterProviderConfiguration.meterProvider));
			});
	}

	@Configuration(proxyBeanMethods = false)
	public static class MultipleSdkLoggerProviderBuilderCustomizersConfig {

		@Bean
		public SdkLoggerProviderBuilderCustomizer customSdkLoggerProviderBuilderCustomizer1() {
			return new NoopSdkLoggerProviderBuilderCustomizer();
		}

		@Bean
		public SdkLoggerProviderBuilderCustomizer customSdkLoggerProviderBuilderCustomizer2() {
			return new NoopSdkLoggerProviderBuilderCustomizer();
		}

	}

	static class NoopSdkLoggerProviderBuilderCustomizer implements SdkLoggerProviderBuilderCustomizer {

		final AtomicInteger called = new AtomicInteger(0);

		@Override
		public void customize(SdkLoggerProviderBuilder builder) {
			this.called.incrementAndGet();
		}

		int called() {
			return this.called.get();
		}

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
		OtlpHttpLogRecordExporter customOtlpHttpLogRecordExporter() {
			return OtlpHttpLogRecordExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomGrpcExporterConfiguration {

		@Bean
		OtlpGrpcLogRecordExporter customOtlpGrpcLogRecordExporter() {
			return OtlpGrpcLogRecordExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomOtlpLoggingConnectionDetailsConfiguration {

		@Bean
		OpenTelemetryLoggingConnectionDetails customOtlpLoggingConnectionDetails() {
			return (transport) -> "https://otel.example.com/v1/logs";
		}

	}

}
