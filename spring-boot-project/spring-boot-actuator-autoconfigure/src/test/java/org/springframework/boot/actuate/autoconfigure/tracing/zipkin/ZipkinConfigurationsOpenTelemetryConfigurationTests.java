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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import org.junit.jupiter.api.Test;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.Sender;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.OpenTelemetryConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OpenTelemetryConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ZipkinConfigurationsOpenTelemetryConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(BaseConfiguration.class, OpenTelemetryConfiguration.class));

	private final ApplicationContextRunner tracingDisabledContextRunner = this.contextRunner
		.withPropertyValues("management.tracing.enabled=false");

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(ZipkinSpanExporter.class));
	}

	@Test
	void shouldNotSupplyZipkinSpanExporterIfSenderIsMissing() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ZipkinSpanExporter.class));
	}

	@Test
	void shouldNotSupplyZipkinSpanExporterIfNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.exporter.zipkin"))
			.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ZipkinSpanExporter.class));

	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customZipkinSpanExporter");
			assertThat(context).hasSingleBean(ZipkinSpanExporter.class);
		});
	}

	@Test
	void shouldNotSupplyZipkinSpanExporterIfTracingIsDisabled() {
		this.tracingDisabledContextRunner.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ZipkinSpanExporter.class));
	}

	@Configuration(proxyBeanMethods = false)
	private static class SenderConfiguration {

		@Bean
		Sender sender() {
			return new NoopSender();
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		ZipkinSpanExporter customZipkinSpanExporter() {
			return ZipkinSpanExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class BaseConfiguration {

		@Bean
		@ConditionalOnMissingBean
		BytesEncoder<Span> spanBytesEncoder() {
			return SpanBytesEncoder.JSON_V2;
		}

	}

}
