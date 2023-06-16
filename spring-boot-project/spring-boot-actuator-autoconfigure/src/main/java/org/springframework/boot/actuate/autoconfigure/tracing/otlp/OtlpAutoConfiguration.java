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

package org.springframework.boot.actuate.autoconfigure.tracing.otlp;

import java.util.Map.Entry;

import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;

import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OTLP. Brave does not support
 * OTLP, so we only configure it for OpenTelemetry. OTLP defines three transports that are
 * supported: gRPC (/protobuf), HTTP/protobuf, HTTP/JSON. From these transports HTTP/JSON
 * is not supported by the OTel Java SDK, and it seems there are no plans supporting it in
 * the future, see: <a href=
 * "https://github.com/open-telemetry/opentelemetry-java/issues/3651">opentelemetry-java#3651</a>.
 * Because this class configures components from the OTel SDK, it can't support HTTP/JSON.
 * To keep things simple, we only auto-configure HTTP/protobuf. If you want to use gRPC,
 * define an {@link OtlpGrpcSpanExporter} and this auto-configuration will back off.
 *
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 * @since 3.1.0
 */
@AutoConfiguration
@ConditionalOnClass({ OtelTracer.class, SdkTracerProvider.class, OpenTelemetry.class, OtlpHttpSpanExporter.class })
@EnableConfigurationProperties(OtlpProperties.class)
public class OtlpAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(value = OtlpHttpSpanExporter.class,
			type = "io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter")
	@ConditionalOnProperty(prefix = "management.otlp.tracing", name = "endpoint")
	@ConditionalOnEnabledTracing
	OtlpHttpSpanExporter otlpHttpSpanExporter(OtlpProperties properties) {
		OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
			.setEndpoint(properties.getEndpoint())
			.setTimeout(properties.getTimeout())
			.setCompression(properties.getCompression().name().toLowerCase());
		for (Entry<String, String> header : properties.getHeaders().entrySet()) {
			builder.addHeader(header.getKey(), header.getValue());
		}
		return builder.build();
	}

}
