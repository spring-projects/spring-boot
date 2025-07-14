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

package org.springframework.boot.tracing.autoconfigure.zipkin;

import brave.Tag;
import brave.Tags;
import brave.handler.MutableSpan;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import zipkin2.Span;
import zipkin2.reporter.BytesEncoder;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.SpanBytesEncoder;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.MutableSpanBytesEncoder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.tracing.autoconfigure.ConditionalOnEnabledTracing;
import org.springframework.boot.tracing.autoconfigure.zipkin.ZipkinTracingAutoConfiguration.BraveConfiguration;
import org.springframework.boot.tracing.autoconfigure.zipkin.ZipkinTracingAutoConfiguration.OpenTelemetryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Zipkin tracing.
 *
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 * @author Wick Dynex
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.zipkin.autoconfigure.ZipkinAutoConfiguration")
@ConditionalOnClass(Encoding.class)
@Import({ BraveConfiguration.class, OpenTelemetryConfiguration.class })
public class ZipkinTracingAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(AsyncZipkinSpanHandler.class)
	static class BraveConfiguration {

		@Bean
		@ConditionalOnBean(Encoding.class)
		@ConditionalOnMissingBean(value = MutableSpan.class, parameterizedContainer = BytesEncoder.class)
		BytesEncoder<MutableSpan> mutableSpanBytesEncoder(Encoding encoding,
				ObjectProvider<Tag<Throwable>> throwableTagProvider) {
			Tag<Throwable> throwableTag = throwableTagProvider.getIfAvailable(() -> Tags.ERROR);
			return MutableSpanBytesEncoder.create(encoding, throwableTag);
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(BytesMessageSender.class)
		@ConditionalOnEnabledTracing("zipkin")
		AsyncZipkinSpanHandler asyncZipkinSpanHandler(BytesMessageSender sender,
				BytesEncoder<MutableSpan> mutableSpanBytesEncoder) {
			return AsyncZipkinSpanHandler.newBuilder(sender).build(mutableSpanBytesEncoder);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ ZipkinSpanExporter.class, Span.class })
	static class OpenTelemetryConfiguration {

		@Bean
		@ConditionalOnBean(Encoding.class)
		@ConditionalOnMissingBean(value = Span.class, parameterizedContainer = BytesEncoder.class)
		BytesEncoder<Span> spanBytesEncoder(Encoding encoding) {
			return SpanBytesEncoder.forEncoding(encoding);
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(BytesMessageSender.class)
		@ConditionalOnEnabledTracing("zipkin")
		ZipkinSpanExporter zipkinSpanExporter(BytesMessageSender sender, BytesEncoder<Span> spanBytesEncoder) {
			return ZipkinSpanExporter.builder().setSender(sender).setEncoder(spanBytesEncoder).build();
		}

	}

}
