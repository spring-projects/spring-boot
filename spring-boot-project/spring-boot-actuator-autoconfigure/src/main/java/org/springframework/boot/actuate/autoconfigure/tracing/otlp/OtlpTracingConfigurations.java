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

package org.springframework.boot.actuate.autoconfigure.tracing.otlp;

import java.util.Map.Entry;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;

import org.springframework.boot.actuate.autoconfigure.opentelemetry.otlp.Transport;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Configurations imported by {@link OtlpAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Eddú Meléndez
 */
class OtlpTracingConfigurations {

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetails {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty(prefix = "management.otlp.tracing", name = "endpoint")
		OtlpTracingConnectionDetails otlpTracingConnectionDetails(OtlpProperties properties) {
			return new PropertiesOtlpTracingConnectionDetails(properties);
		}

		/**
		 * Adapts {@link OtlpProperties} to {@link OtlpTracingConnectionDetails}.
		 */
		static class PropertiesOtlpTracingConnectionDetails implements OtlpTracingConnectionDetails {

			private final OtlpProperties properties;

			PropertiesOtlpTracingConnectionDetails(OtlpProperties properties) {
				this.properties = properties;
			}

			@Override
			public String getUrl(Transport transport) {
				Assert.state(transport == this.properties.getTransport(),
						"Requested transport %s doesn't match configured transport %s".formatted(transport,
								this.properties.getTransport()));
				return this.properties.getEndpoint();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean({ OtlpGrpcSpanExporter.class, OtlpHttpSpanExporter.class })
	@ConditionalOnBean(OtlpTracingConnectionDetails.class)
	@ConditionalOnEnabledTracing("otlp")
	static class Exporters {

		@Bean
		@ConditionalOnProperty(prefix = "management.otlp.tracing", name = "transport", havingValue = "http",
				matchIfMissing = true)
		OtlpHttpSpanExporter otlpHttpSpanExporter(OtlpProperties properties,
				OtlpTracingConnectionDetails connectionDetails) {
			OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.HTTP))
				.setTimeout(properties.getTimeout())
				.setCompression(properties.getCompression().name().toLowerCase());
			for (Entry<String, String> header : properties.getHeaders().entrySet()) {
				builder.addHeader(header.getKey(), header.getValue());
			}
			return builder.build();
		}

		@Bean
		@ConditionalOnProperty(prefix = "management.otlp.tracing", name = "transport", havingValue = "grpc")
		OtlpGrpcSpanExporter otlpGrpcSpanExporter(OtlpProperties properties,
				OtlpTracingConnectionDetails connectionDetails) {
			OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.GRPC))
				.setTimeout(properties.getTimeout())
				.setCompression(properties.getCompression().name().toLowerCase());
			for (Entry<String, String> header : properties.getHeaders().entrySet()) {
				builder.addHeader(header.getKey(), header.getValue());
			}
			return builder.build();
		}

	}

}
