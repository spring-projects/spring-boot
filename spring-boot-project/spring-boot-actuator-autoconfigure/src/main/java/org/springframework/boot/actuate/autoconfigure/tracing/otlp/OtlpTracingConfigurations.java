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

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;

import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurations imported by {@link OtlpAutoConfiguration}.
 *
 * @author Moritz Halbritter
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
			public String getUrl() {
				return this.properties.getEndpoint();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Exporters {

		@Bean
		@ConditionalOnMissingBean(value = OtlpHttpSpanExporter.class,
				type = "io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter")
		@ConditionalOnBean(OtlpTracingConnectionDetails.class)
		@ConditionalOnEnabledTracing
		OtlpHttpSpanExporter otlpHttpSpanExporter(OtlpProperties properties,
				OtlpTracingConnectionDetails connectionDetails) {
			OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
				.setEndpoint(getUrl(connectionDetails))
				.setTimeout(properties.getTimeout())
				.setCompression(properties.getCompression().name().toLowerCase());
			for (Entry<String, String> header : getHeaders(properties).entrySet()) {
				builder.addHeader(header.getKey(), header.getValue());
			}
			return builder.build();
		}

	}

	private static String getUrl(OtlpTracingConnectionDetails connectionDetails) {
		String url = connectionDetails.getUrl();
		if (url != null) {
			return url;
		}
		Map<String, String> env = System.getenv();
	            String endpoint = env.get("OTEL_EXPORTER_OTLP_TRACES_ENDPOINT");
	            if (endpoint == null) {
	                endpoint = env.get("OTEL_EXPORTER_OTLP_ENDPOINT");
	            }
	            if (endpoint == null) {
	                endpoint = "http://localhost:4318/v1/traces";
	            }
	            else if (!endpoint.endsWith("/v1/traces")) {
	                endpoint = endpoint + "/v1/traces";
	            }
	            return endpoint;
	}

	private static Map<String, String> getHeaders(OtlpProperties properties) {
		Map<String, String> headers = properties.getHeaders();
		if (!headers.isEmpty()) {
			return headers;
		}

            Map<String, String> env = System.getenv();
            // common headers
		String headersString = env.getOrDefault("OTEL_EXPORTER_OTLP_HEADERS", "").trim();
            String metricsHeaders = env.getOrDefault("OTEL_EXPORTER_OTLP_METRICS_HEADERS", "").trim();
            headersString = Objects.equals(headersString, "") ? metricsHeaders : headersString + "," + metricsHeaders;

        String[] keyValues = Objects.equals(headersString, "") ? new String[] {} : headersString.split(",");

        return Arrays.stream(keyValues)
            .map(String::trim)
            .filter(keyValue -> keyValue.length() > 2 && keyValue.indexOf('=') > 0)
            .collect(Collectors.toMap(keyValue -> keyValue.substring(0, keyValue.indexOf('=')).trim(),
                    keyValue -> keyValue.substring(keyValue.indexOf('=') + 1).trim(), (l, r) -> r));
	}
}
