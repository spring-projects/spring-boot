/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Locale;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;

import org.springframework.boot.actuate.autoconfigure.logging.ConditionalOnEnabledLoggingExport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Configurations imported by {@link OtlpLoggingAutoConfiguration}.
 *
 * @author Toshiaki Maki
 */
final class OtlpLoggingConfigurations {

	private OtlpLoggingConfigurations() {
	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetails {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty("management.otlp.logging.endpoint")
		OtlpLoggingConnectionDetails otlpLoggingConnectionDetails(OtlpLoggingProperties properties) {
			return new PropertiesOtlpLoggingConnectionDetails(properties);
		}

		/**
		 * Adapts {@link OtlpLoggingProperties} to {@link OtlpLoggingConnectionDetails}.
		 */
		static class PropertiesOtlpLoggingConnectionDetails implements OtlpLoggingConnectionDetails {

			private final OtlpLoggingProperties properties;

			PropertiesOtlpLoggingConnectionDetails(OtlpLoggingProperties properties) {
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
	@ConditionalOnMissingBean({ OtlpGrpcLogRecordExporter.class, OtlpHttpLogRecordExporter.class })
	@ConditionalOnBean(OtlpLoggingConnectionDetails.class)
	@ConditionalOnEnabledLoggingExport("otlp")
	static class Exporters {

		@Bean
		@ConditionalOnProperty(name = "management.otlp.logging.transport", havingValue = "http", matchIfMissing = true)
		OtlpHttpLogRecordExporter otlpHttpLogRecordExporter(OtlpLoggingProperties properties,
				OtlpLoggingConnectionDetails connectionDetails) {
			OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.HTTP))
				.setTimeout(properties.getTimeout())
				.setConnectTimeout(properties.getConnectTimeout())
				.setCompression(properties.getCompression().name().toLowerCase(Locale.US));
			properties.getHeaders().forEach(builder::addHeader);
			return builder.build();
		}

		@Bean
		@ConditionalOnProperty(name = "management.otlp.logging.transport", havingValue = "grpc")
		OtlpGrpcLogRecordExporter otlpGrpcLogRecordExporter(OtlpLoggingProperties properties,
				OtlpLoggingConnectionDetails connectionDetails) {
			OtlpGrpcLogRecordExporterBuilder builder = OtlpGrpcLogRecordExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.GRPC))
				.setTimeout(properties.getTimeout())
				.setConnectTimeout(properties.getConnectTimeout())
				.setCompression(properties.getCompression().name().toLowerCase(Locale.US));
			properties.getHeaders().forEach(builder::addHeader);
			return builder.build();
		}

	}

}
