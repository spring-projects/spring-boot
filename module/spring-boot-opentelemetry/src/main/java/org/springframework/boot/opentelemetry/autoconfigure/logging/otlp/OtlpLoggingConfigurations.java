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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.opentelemetry.autoconfigure.OtlpProperties;
import org.springframework.boot.opentelemetry.autoconfigure.logging.ConditionalOnEnabledLoggingExport;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
		@ConditionalOnMissingBean(OtlpLoggingConnectionDetails.class)
		@Conditional(OtlpEndpointCondition.class)
		PropertiesOtlpLoggingConnectionDetails openTelemetryLoggingConnectionDetails(OtlpLoggingProperties properties,
				OtlpProperties otlpProperties, ObjectProvider<SslBundles> sslBundles) {
			return new PropertiesOtlpLoggingConnectionDetails(properties, otlpProperties, sslBundles.getIfAvailable());
		}

		/**
		 * Condition to check if either the logging-specific endpoint or the common OTLP
		 * endpoint is set.
		 */
		static class OtlpEndpointCondition extends AnyNestedCondition {

			OtlpEndpointCondition() {
				super(ConfigurationPhase.REGISTER_BEAN);
			}

			@ConditionalOnProperty("management.opentelemetry.logging.export.otlp.endpoint")
			@SuppressWarnings("unused")
			static class LoggingEndpoint {

			}

			@ConditionalOnProperty("management.opentelemetry.otlp.endpoint")
			@SuppressWarnings("unused")
			static class CommonEndpoint {

			}

		}

		/**
		 * Adapts {@link OtlpLoggingProperties} and {@link OtlpProperties} to
		 * {@link OtlpLoggingConnectionDetails}.
		 */
		static class PropertiesOtlpLoggingConnectionDetails implements OtlpLoggingConnectionDetails {

			private final OtlpLoggingProperties properties;

			private final OtlpProperties otlpProperties;

			private final @Nullable SslBundles sslBundles;

			PropertiesOtlpLoggingConnectionDetails(OtlpLoggingProperties properties, OtlpProperties otlpProperties,
					@Nullable SslBundles sslBundles) {
				this.properties = properties;
				this.otlpProperties = otlpProperties;
				this.sslBundles = sslBundles;
			}

			@Override
			public String getUrl(Transport transport) {
				String endpoint = this.properties.getEndpoint();
				if (!StringUtils.hasLength(endpoint)) {
					endpoint = this.otlpProperties.getEndpoint();
					if (endpoint != null && transport == Transport.HTTP) {
						endpoint = endpoint.endsWith("/") ? endpoint + "v1/logs" : endpoint + "/v1/logs";
					}
				}
				Assert.state(endpoint != null, "'endpoint' must not be null");
				return endpoint;
			}

			@Override
			public @Nullable SslBundle getSslBundle() {
				String bundleName = this.properties.getSsl().getBundle();
				if (StringUtils.hasLength(bundleName)) {
					Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
					return this.sslBundles.getBundle(bundleName);
				}
				return null;
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
				OtlpProperties otlpProperties, OtlpLoggingConnectionDetails connectionDetails,
				ObjectProvider<MeterProvider> meterProvider,
				ObjectProvider<OtlpHttpLogRecordExporterBuilderCustomizer> customizers) {
			OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.HTTP));

			Duration timeout = properties.getTimeout();
			builder.setTimeout(timeout);

			String compression = properties.getCompression().name().toLowerCase(Locale.ROOT);
			if (StringUtils.hasLength(compression)) {
				builder.setCompression(compression);
			}

			Map<String, String> headers = new LinkedHashMap<>(otlpProperties.getHeaders());
			headers.putAll(properties.getHeaders());
			headers.forEach(builder::addHeader);

			meterProvider.ifAvailable(builder::setMeterProvider);
			configureSsl(connectionDetails, builder::setSslContext);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder.build();
		}

		@Bean
		@ConditionalOnProperty(name = "management.opentelemetry.logging.export.otlp.transport", havingValue = "grpc")
		OtlpGrpcLogRecordExporter otlpGrpcLogRecordExporter(OtlpLoggingProperties properties,
				OtlpProperties otlpProperties, OtlpLoggingConnectionDetails connectionDetails,
				ObjectProvider<MeterProvider> meterProvider,
				ObjectProvider<OtlpGrpcLogRecordExporterBuilderCustomizer> customizers) {
			OtlpGrpcLogRecordExporterBuilder builder = OtlpGrpcLogRecordExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.GRPC));

			Duration timeout = properties.getTimeout();
			builder.setTimeout(timeout);

			String compression = properties.getCompression().name().toLowerCase(Locale.ROOT);
			if (StringUtils.hasLength(compression)) {
				builder.setCompression(compression);
			}

			Map<String, String> headers = new LinkedHashMap<>(otlpProperties.getHeaders());
			headers.putAll(properties.getHeaders());
			headers.forEach(builder::addHeader);

			meterProvider.ifAvailable(builder::setMeterProvider);
			configureSsl(connectionDetails, builder::setSslContext);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder.build();
		}

		private void configureSsl(OtlpLoggingConnectionDetails connectionDetails,
				SslContextConfigurer sslContextConfigurer) {
			SslBundle sslBundle = connectionDetails.getSslBundle();
			if (sslBundle != null) {
				SSLContext sslContext = sslBundle.createSslContext();
				X509TrustManager trustManager = extractTrustManager(sslBundle);
				sslContextConfigurer.configure(sslContext, trustManager);
			}
		}

		private X509TrustManager extractTrustManager(SslBundle sslBundle) {
			for (TrustManager trustManager : sslBundle.getManagers().getTrustManagers()) {
				if (trustManager instanceof X509TrustManager x509TrustManager) {
					return x509TrustManager;
				}
			}
			throw new IllegalStateException("No X509TrustManager found in the SSL bundle trust managers");
		}

		private interface SslContextConfigurer {

			void configure(SSLContext sslContext, X509TrustManager trustManager);

		}

	}

}
