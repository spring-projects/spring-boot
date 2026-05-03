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

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp;

import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.micrometer.tracing.autoconfigure.ConditionalOnEnabledTracingExport;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Configurations imported by {@link OtlpTracingAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Eddú Meléndez
 */
final class OtlpTracingConfigurations {

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetails {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnProperty("management.opentelemetry.tracing.export.otlp.endpoint")
		OtlpTracingConnectionDetails otlpTracingConnectionDetails(OtlpTracingProperties properties,
				ObjectProvider<SslBundles> sslBundles) {
			return new PropertiesOtlpTracingConnectionDetails(properties, sslBundles.getIfAvailable());
		}

		/**
		 * Adapts {@link OtlpTracingProperties} to {@link OtlpTracingConnectionDetails}.
		 */
		static class PropertiesOtlpTracingConnectionDetails implements OtlpTracingConnectionDetails {

			private final OtlpTracingProperties properties;

			private final @Nullable SslBundles sslBundles;

			PropertiesOtlpTracingConnectionDetails(OtlpTracingProperties properties, @Nullable SslBundles sslBundles) {
				this.properties = properties;
				this.sslBundles = sslBundles;
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
	@ConditionalOnMissingBean({ OtlpGrpcSpanExporter.class, OtlpHttpSpanExporter.class })
	@ConditionalOnBean(OtlpTracingConnectionDetails.class)
	@ConditionalOnEnabledTracingExport("otlp")
	static class Exporters {

		@Bean
		@ConditionalOnProperty(name = "management.opentelemetry.tracing.export.otlp.transport", havingValue = "http",
				matchIfMissing = true)
		OtlpHttpSpanExporter otlpHttpSpanExporter(OtlpTracingProperties properties,
				OtlpTracingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider,
				ObjectProvider<OtlpHttpSpanExporterBuilderCustomizer> customizers) {
			OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.HTTP))
				.setTimeout(properties.getTimeout())
				.setConnectTimeout(properties.getConnectTimeout())
				.setCompression(properties.getCompression().name().toLowerCase(Locale.ROOT));
			properties.getHeaders().forEach(builder::addHeader);
			meterProvider.ifAvailable(builder::setMeterProvider);
			configureSsl(connectionDetails, builder::setSslContext);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder.build();
		}

		@Bean
		@ConditionalOnProperty(name = "management.opentelemetry.tracing.export.otlp.transport", havingValue = "grpc")
		OtlpGrpcSpanExporter otlpGrpcSpanExporter(OtlpTracingProperties properties,
				OtlpTracingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider,
				ObjectProvider<OtlpGrpcSpanExporterBuilderCustomizer> customizers) {
			OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.GRPC))
				.setTimeout(properties.getTimeout())
				.setConnectTimeout(properties.getConnectTimeout())
				.setCompression(properties.getCompression().name().toLowerCase(Locale.ROOT));
			properties.getHeaders().forEach(builder::addHeader);
			meterProvider.ifAvailable(builder::setMeterProvider);
			configureSsl(connectionDetails, builder::setSslContext);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder.build();
		}

		private void configureSsl(OtlpTracingConnectionDetails connectionDetails,
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
