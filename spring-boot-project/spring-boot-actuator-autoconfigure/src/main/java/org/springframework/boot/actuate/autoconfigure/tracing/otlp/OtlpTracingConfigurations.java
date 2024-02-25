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

	/**
	 * ConnectionDetails class.
	 */
	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetails {

		/**
		 * Creates an instance of {@link OtlpTracingConnectionDetails} based on the
		 * provided {@link OtlpProperties}. This method is annotated with {@link Bean},
		 * {@link ConditionalOnMissingBean}, and {@link ConditionalOnProperty} to ensure
		 * that it is only executed when the specified conditions are met.
		 * @param properties the {@link OtlpProperties} used to configure the connection
		 * details
		 * @return an instance of {@link OtlpTracingConnectionDetails} with the provided
		 * properties
		 */
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

			/**
			 * Constructs a new instance of PropertiesOtlpTracingConnectionDetails with
			 * the specified properties.
			 * @param properties the OTLP properties to be used for the connection details
			 */
			PropertiesOtlpTracingConnectionDetails(OtlpProperties properties) {
				this.properties = properties;
			}

			/**
			 * Returns the URL of the endpoint for the OTLP tracing connection.
			 * @return the URL of the endpoint
			 */
			@Override
			public String getUrl() {
				return this.properties.getEndpoint();
			}

		}

	}

	/**
	 * Exporters class.
	 */
	@Configuration(proxyBeanMethods = false)
	static class Exporters {

		/**
		 * Creates an instance of {@link OtlpHttpSpanExporter} if no bean of type
		 * {@link OtlpHttpSpanExporter} is already present and if a bean of type
		 * {@link OtlpTracingConnectionDetails} is present and tracing is enabled.
		 *
		 * The method sets the endpoint, timeout, and compression properties of the
		 * exporter using the provided {@link OtlpProperties} and
		 * {@link OtlpTracingConnectionDetails}. It also adds any custom headers specified
		 * in the {@link OtlpProperties}.
		 * @param properties The {@link OtlpProperties} containing the exporter
		 * configuration.
		 * @param connectionDetails The {@link OtlpTracingConnectionDetails} containing
		 * the connection details for the exporter.
		 * @return An instance of {@link OtlpHttpSpanExporter} configured with the
		 * provided properties and connection details.
		 */
		@Bean
		@ConditionalOnMissingBean(value = OtlpHttpSpanExporter.class,
				type = "io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter")
		@ConditionalOnBean(OtlpTracingConnectionDetails.class)
		@ConditionalOnEnabledTracing
		OtlpHttpSpanExporter otlpHttpSpanExporter(OtlpProperties properties,
				OtlpTracingConnectionDetails connectionDetails) {
			OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
				.setEndpoint(connectionDetails.getUrl())
				.setTimeout(properties.getTimeout())
				.setCompression(properties.getCompression().name().toLowerCase());
			for (Entry<String, String> header : properties.getHeaders().entrySet()) {
				builder.addHeader(header.getKey(), header.getValue());
			}
			return builder.build();
		}

	}

}
