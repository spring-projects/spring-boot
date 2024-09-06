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

package org.springframework.boot.actuate.autoconfigure.logging.opentelemetry.otlp;

import java.util.Locale;

import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;

import org.springframework.boot.actuate.autoconfigure.opentelemetry.otlp.Transport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
		@ConditionalOnProperty(prefix = "management.otlp.logging", name = "endpoint")
		OtlpLoggingConnectionDetails otlpLogsConnectionDetails(OtlpLoggingProperties properties) {
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
				return this.properties.getEndpoint();
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Exporters {

		@ConditionalOnMissingBean(value = OtlpHttpLogRecordExporter.class,
				type = "io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter")
		@ConditionalOnBean(OtlpLoggingConnectionDetails.class)
		@Bean
		OtlpHttpLogRecordExporter otlpHttpLogRecordExporter(OtlpLoggingProperties properties,
				OtlpLoggingConnectionDetails connectionDetails) {
			OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.HTTP))
				.setCompression(properties.getCompression().name().toLowerCase(Locale.US))
				.setTimeout(properties.getTimeout());
			properties.getHeaders().forEach(builder::addHeader);
			return builder.build();
		}

	}

}
