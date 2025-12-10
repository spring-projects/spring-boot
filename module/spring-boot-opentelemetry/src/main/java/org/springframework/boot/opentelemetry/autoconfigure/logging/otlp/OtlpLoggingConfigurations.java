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

package org.springframework.boot.opentelemetry.autoconfigure.logging.otlp;

import java.util.Locale;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.opentelemetry.autoconfigure.logging.ConditionalOnEnabledLoggingExport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Configurations imported by {@link OtlpLoggingAutoConfiguration}.
 *
 * @author Toshiaki Maki
 * @author Moritz Halbritter
 */
final class OtlpLoggingConfigurations {

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetails {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty("management.opentelemetry.logging.export.otlp.endpoint")
		PropertiesOtlpLoggingConnectionDetails openTelemetryLoggingConnectionDetails(OtlpLoggingProperties properties) {
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
				String endpoint = this.properties.getEndpoint();
				Assert.state(endpoint != null, "'endpoint' must not be null");
				return endpoint;
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(OtlpHttpLogRecordExporter.class)
	@ConditionalOnMissingBean({ OtlpGrpcLogRecordExporter.class, OtlpHttpLogRecordExporter.class })
	@ConditionalOnBean(OtlpLoggingConnectionDetails.class)
	@ConditionalOnEnabledLoggingExport("otlp")
	static class Exporters {

		@Bean
		@ConditionalOnProperty(name = "management.opentelemetry.logging.export.otlp.transport", havingValue = "http",
				matchIfMissing = true)
		OtlpHttpLogRecordExporter otlpHttpLogRecordExporter(OtlpLoggingProperties properties,
				OtlpLoggingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
			OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.HTTP))
				.setTimeout(properties.getTimeout())
				.setConnectTimeout(properties.getConnectTimeout())
				.setCompression(properties.getCompression().name().toLowerCase(Locale.US));
			properties.getHeaders().forEach(builder::addHeader);
			meterProvider.ifAvailable(builder::setMeterProvider);
			return builder.build();
		}

		@Bean
		@ConditionalOnProperty(name = "management.opentelemetry.logging.export.otlp.transport", havingValue = "grpc")
		OtlpGrpcLogRecordExporter otlpGrpcLogRecordExporter(OtlpLoggingProperties properties,
				OtlpLoggingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider) {
			OtlpGrpcLogRecordExporterBuilder builder = OtlpGrpcLogRecordExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.GRPC))
				.setTimeout(properties.getTimeout())
				.setConnectTimeout(properties.getConnectTimeout())
				.setCompression(properties.getCompression().name().toLowerCase(Locale.US));
			properties.getHeaders().forEach(builder::addHeader);
			meterProvider.ifAvailable(builder::setMeterProvider);
			return builder.build();
		}

	}

}
