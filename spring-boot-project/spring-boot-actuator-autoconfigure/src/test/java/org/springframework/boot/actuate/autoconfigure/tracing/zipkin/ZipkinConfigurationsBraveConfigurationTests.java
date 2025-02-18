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

import java.nio.charset.StandardCharsets;

import brave.Tag;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import org.junit.jupiter.api.Test;
import zipkin2.reporter.BytesEncoder;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.BraveConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BraveConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ZipkinConfigurationsBraveConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DefaultEncodingConfiguration.class, BraveConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(AsyncZipkinSpanHandler.class));
	}

	@Test
	void shouldNotSupplySpanHandlerIfReporterIsMissing() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(AsyncZipkinSpanHandler.class));
	}

	@Test
	void shouldNotSupplyIfZipkinReporterBraveIsNotOnClasspath() {
		// Note: Technically, Brave can work without zipkin-reporter. For example,
		// WavefrontSpanHandler doesn't require this to operate. If we remove this
		// dependency enforcement when WavefrontSpanHandler is in use, we can resolve
		// micrometer-metrics/tracing#509. We also need this for any configuration that
		// uses senders defined in the Spring Boot source tree, such as HttpSender.
		this.contextRunner.withClassLoader(new FilteredClassLoader("zipkin2.reporter.brave"))
			.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(AsyncZipkinSpanHandler.class));
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class, CustomConfiguration.class)
			.run((context) -> {
				assertThat(context).hasBean("customAsyncZipkinSpanHandler");
				assertThat(context).hasSingleBean(AsyncZipkinSpanHandler.class);
			});
	}

	@Test
	void shouldSupplyAsyncZipkinSpanHandlerWithCustomSpanHandler() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class, CustomSpanHandlerConfiguration.class)
			.run((context) -> {
				assertThat(context).hasBean("customSpanHandler");
				assertThat(context).hasSingleBean(AsyncZipkinSpanHandler.class);
			});
	}

	@Test
	void shouldNotSupplyAsyncZipkinSpanHandlerIfGlobalTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.tracing.enabled=false")
			.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(AsyncZipkinSpanHandler.class));
	}

	@Test
	void shouldNotSupplyAsyncZipkinSpanHandlerIfZipkinTracingIsDisabled() {
		this.contextRunner.withPropertyValues("management.zipkin.tracing.export.enabled=false")
			.withUserConfiguration(SenderConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(AsyncZipkinSpanHandler.class));
	}

	@Test
	void shouldUseCustomEncoderBean() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class, CustomEncoderConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(AsyncZipkinSpanHandler.class);
				assertThat(context.getBean(AsyncZipkinSpanHandler.class)).extracting("spanReporter.encoder")
					.isInstanceOf(CustomMutableSpanEncoder.class)
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
				assertThat(context).hasSingleBean(AsyncZipkinSpanHandler.class);
				assertThat(context.getBean(AsyncZipkinSpanHandler.class)).extracting("encoding")
					.isEqualTo(Encoding.PROTO3);
			});
	}

	@Test
	void shouldUseDefaultThrowableTagBean() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class).run((context) -> {
			@SuppressWarnings("unchecked")
			BytesEncoder<MutableSpan> encoder = context.getBean(BytesEncoder.class);
			MutableSpan span = createTestSpan();
			// default tag key name is "error", and doesn't overwrite
			assertThat(new String(encoder.encode(span), StandardCharsets.UTF_8)).isEqualTo(
					"{\"traceId\":\"0000000000000001\",\"id\":\"0000000000000001\",\"tags\":{\"error\":\"true\"}}");
		});
	}

	@Test
	void shouldUseCustomThrowableTagBean() {
		this.contextRunner.withUserConfiguration(SenderConfiguration.class, CustomThrowableTagConfiguration.class)
			.run((context) -> {
				@SuppressWarnings("unchecked")
				BytesEncoder<MutableSpan> encoder = context.getBean(BytesEncoder.class);
				MutableSpan span = createTestSpan();
				// The custom throwable parser doesn't use the key "error" we can see both
				assertThat(new String(encoder.encode(span), StandardCharsets.UTF_8)).isEqualTo(
						"{\"traceId\":\"0000000000000001\",\"id\":\"0000000000000001\",\"tags\":{\"error\":\"true\",\"exception\":\"ice cream\"}}");
			});
	}

	private MutableSpan createTestSpan() {
		MutableSpan span = new MutableSpan();
		span.traceId("1");
		span.id("1");
		span.tag("error", "true");
		span.error(new RuntimeException("ice cream"));
		return span;
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
		AsyncZipkinSpanHandler customAsyncZipkinSpanHandler() {
			return AsyncZipkinSpanHandler.create(new NoopSender(Encoding.JSON));
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomThrowableTagConfiguration {

		@Bean
		Tag<Throwable> throwableTag() {
			return new Tag<>("exception") {
				@Override
				protected String parseValue(Throwable throwable, TraceContext traceContext) {
					return throwable.getMessage();
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomSpanHandlerConfiguration {

		@Bean
		SpanHandler customSpanHandler() {
			return mock(SpanHandler.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomEncodingConfiguration {

		@Bean
		Encoding encoding() {
			return Encoding.PROTO3;
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomEncoderConfiguration {

		@Bean
		BytesEncoder<MutableSpan> encoder(Encoding encoding) {
			return new CustomMutableSpanEncoder(encoding);
		}

	}

	private record CustomMutableSpanEncoder(Encoding encoding) implements BytesEncoder<MutableSpan> {

		@Override
		public int sizeInBytes(MutableSpan span) {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] encode(MutableSpan span) {
			throw new UnsupportedOperationException();
		}

	}

}
