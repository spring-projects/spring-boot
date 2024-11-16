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

package org.springframework.boot.actuate.autoconfigure.logging.otlp;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.actuate.autoconfigure.logging.otlp.OtlpLoggingConfigurations.ConnectionDetails.PropertiesOtlpLoggingConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OtlpLoggingAutoConfiguration}.
 *
 * @author Toshiaki Maki
 */
class OtlpLoggingAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OtlpLoggingAutoConfiguration.class));

	@Test
	void shouldNotSupplyBeansIfPropertyIsNotSet() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(OtlpLoggingConnectionDetails.class);
			assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
		});
	}

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withPropertyValues("management.otlp.logging.endpoint=http://localhost:4318/v1/logs")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);
				OtlpLoggingConnectionDetails connectionDetails = context.getBean(OtlpLoggingConnectionDetails.class);
				assertThat(connectionDetails.getUrl(Transport.HTTP)).isEqualTo("http://localhost:4318/v1/logs");
				assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class)
					.hasSingleBean(LogRecordExporter.class);
			});
	}

	@ParameterizedTest
	@ValueSource(strings = { "io.opentelemetry.sdk.logs", "io.opentelemetry.api",
			"io.opentelemetry.exporter.otlp.http.logs" })
	void shouldNotSupplyBeansIfDependencyIsMissing(String packageName) {
		this.contextRunner.withClassLoader(new FilteredClassLoader(packageName)).run((context) -> {
			assertThat(context).doesNotHaveBean(OtlpLoggingConnectionDetails.class);
			assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
		});
	}

	@Test
	void shouldBackOffWhenLoggingExportPropertyIsNotEnabled() {
		this.contextRunner
			.withPropertyValues("management.logging.export.enabled=false",
					"management.otlp.logging.endpoint=http://localhost:4318/v1/logs")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);
				assertThat(context).doesNotHaveBean(LogRecordExporter.class);
			});
	}

	@Test
	void shouldBackOffWhenOtlpLoggingExportPropertyIsNotEnabled() {
		this.contextRunner
			.withPropertyValues("management.otlp.logging.export.enabled=false",
					"management.otlp.logging.endpoint=http://localhost:4318/v1/logs")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);
				assertThat(context).doesNotHaveBean(LogRecordExporter.class);
			});
	}

	@Test
	void shouldBackOffWhenCustomHttpExporterIsDefined() {
		this.contextRunner.withUserConfiguration(CustomHttpExporterConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customOtlpHttpLogRecordExporter")
				.hasSingleBean(LogRecordExporter.class));
	}

	@Test
	void shouldBackOffWhenCustomGrpcExporterIsDefined() {
		this.contextRunner.withUserConfiguration(CustomGrpcExporterConfiguration.class)
			.run((context) -> assertThat(context).hasBean("customOtlpGrpcLogRecordExporter")
				.hasSingleBean(LogRecordExporter.class));
	}

	@Test
	void shouldBackOffWhenCustomOtlpLoggingConnectionDetailsIsDefined() {
		this.contextRunner.withUserConfiguration(CustomOtlpLoggingConnectionDetails.class).run((context) -> {
			assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class)
				.doesNotHaveBean(PropertiesOtlpLoggingConnectionDetails.class);
			OtlpHttpLogRecordExporter otlpHttpLogRecordExporter = context.getBean(OtlpHttpLogRecordExporter.class);
			assertThat(otlpHttpLogRecordExporter).extracting("delegate.httpSender.url")
				.isEqualTo(HttpUrl.get("https://otel.example.com/v1/logs"));
		});

	}

	@Test
	void shouldUseHttpExporterIfTransportIsNotSet() {
		this.contextRunner.withPropertyValues("management.otlp.logging.endpoint=http://localhost:4318/v1/logs")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class)
					.hasSingleBean(LogRecordExporter.class);
				assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
			});
	}

	@Test
	void shouldUseHttpExporterIfTransportIsSetToHttp() {
		this.contextRunner
			.withPropertyValues("management.otlp.logging.endpoint=http://localhost:4318/v1/logs",
					"management.otlp.logging.transport=http")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class)
					.hasSingleBean(LogRecordExporter.class);
				assertThat(context).doesNotHaveBean(OtlpGrpcLogRecordExporter.class);
			});
	}

	@Test
	void shouldUseGrpcExporterIfTransportIsSetToGrpc() {
		this.contextRunner
			.withPropertyValues("management.otlp.logging.endpoint=http://localhost:4318/v1/logs",
					"management.otlp.logging.transport=grpc")
			.run((context) -> {
				assertThat(context).hasSingleBean(OtlpGrpcLogRecordExporter.class)
					.hasSingleBean(LogRecordExporter.class);
				assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	public static class CustomHttpExporterConfiguration {

		@Bean
		public OtlpHttpLogRecordExporter customOtlpHttpLogRecordExporter() {
			return OtlpHttpLogRecordExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class CustomGrpcExporterConfiguration {

		@Bean
		public OtlpGrpcLogRecordExporter customOtlpGrpcLogRecordExporter() {
			return OtlpGrpcLogRecordExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class CustomOtlpLoggingConnectionDetails {

		@Bean
		public OtlpLoggingConnectionDetails customOtlpLoggingConnectionDetails() {
			return (transport) -> "https://otel.example.com/v1/logs";
		}

	}

}
