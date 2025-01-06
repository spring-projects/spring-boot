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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import org.junit.jupiter.api.Test;
import zipkin2.Span;
import zipkin2.reporter.BytesEncoder;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.OpenTelemetryConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
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
		.withConfiguration(AutoConfigurations.of(DefaultEncodingConfiguration.class, OpenTelemetryConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class, CustomEncoderConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ZipkinSpanExporter.class);
				assertThat(context).hasBean("customSpanEncoder");
			});
	}

	@Test
	void shouldNotSupplyZipkinSpanExporterIfSenderIsMissing() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(ZipkinSpanExporter.class);
			assertThat(context).hasBean("spanBytesEncoder");
		});
	}

	@Test
	void shouldNotSupplyZipkinSpanExporterIfNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.opentelemetry.exporter.zipkin"))
			.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ZipkinSpanExporter.class);
				assertThat(context).doesNotHaveBean("spanBytesEncoder");
			});

	}

	@Test
	void shouldBackOffIfZipkinIsNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("zipkin2.Span"))
			.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ZipkinSpanExporter.class);
				assertThat(context).doesNotHaveBean("spanBytesEncoder");
			});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customZipkinSpanExporter");
			assertThat(context).hasSingleBean(ZipkinSpanExporter.class);
		});
	}

	@Test
	void shouldNotSupplyZipkinSpanExporterIfGlobalTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.enabled=false")
			.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ZipkinSpanExporter.class));
	}

	@Test
	void shouldNotSupplyZipkinSpanExporterIfZipkinTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.zipkin.tracing.export.enabled=false")
			.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ZipkinSpanExporter.class));
	}

	@Test
	void shouldUseCustomEncoderBean() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class, CustomEncoderConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ZipkinSpanExporter.class);
				assertThat(context).hasBean("customSpanEncoder");
				assertThat(context.getBean(ZipkinSpanExporter.class)).extracting("encoder")
					.isInstanceOf(CustomSpanEncoder.class)
					.extracting("encoding")
					.isEqualTo(Encoding.JSON);
			});
	}

	@Test
	void shouldUseCustomEncodingBean() {
		this.contextRunner
			.withUserConfiguration(SenderConfiguration.class, CustomEncodingConfiguration.class,
					CustomEncoderConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ZipkinSpanExporter.class);
				assertThat(context).hasBean("customSpanEncoder");
				assertThat(context.getBean(ZipkinSpanExporter.class)).extracting("encoder")
					.isInstanceOf(CustomSpanEncoder.class)
					.extracting("encoding")
					.isEqualTo(Encoding.PROTO3);
			});
	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomEncodingConfiguration {

		@Bean
		Encoding encoding() {
			return Encoding.PROTO3;
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class SenderConfiguration {

		@Bean
		BytesMessageSender sender(Encoding encoding) {
			return new NoopSender(encoding);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomConfiguration {

		@Bean
		ZipkinSpanExporter customZipkinSpanExporter() {
			return ZipkinSpanExporter.builder().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomEncoderConfiguration {

		@Bean
		BytesEncoder<Span> customSpanEncoder(Encoding encoding) {
			return new CustomSpanEncoder(encoding);
		}

	}

	record CustomSpanEncoder(Encoding encoding) implements BytesEncoder<Span> {

		@Override
		public int sizeInBytes(Span span) {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] encode(Span span) {
			throw new UnsupportedOperationException();
		}

	}

}
