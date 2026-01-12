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

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.zipkin;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import zipkin2.Span;
import zipkin2.reporter.BytesEncoder;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.SpanBytesEncoder;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.micrometer.tracing.autoconfigure.ConditionalOnEnabledTracingExport;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Zipkin tracing with
 * OpenTelemetry.
 *
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 * @author Wick Dynex
 * @author Phillip Webb
 * @since 4.0.0
 */
@AutoConfiguration(afterName = "org.springframework.boot.zipkin.autoconfigure.ZipkinAutoConfiguration")
@ConditionalOnClass({ ZipkinSpanExporter.class, Span.class })
public final class ZipkinWithOpenTelemetryTracingAutoConfiguration {

	@Bean
	@ConditionalOnBean(Encoding.class)
	@ConditionalOnMissingBean(value = Span.class, parameterizedContainer = BytesEncoder.class)
	BytesEncoder<Span> spanBytesEncoder(Encoding encoding) {
		return SpanBytesEncoder.forEncoding(encoding);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(BytesMessageSender.class)
	@ConditionalOnEnabledTracingExport("zipkin")
	ZipkinSpanExporter zipkinSpanExporter(BytesMessageSender sender, BytesEncoder<Span> spanBytesEncoder) {
		return ZipkinSpanExporter.builder().setSender(sender).setEncoder(spanBytesEncoder).build();
	}

}
