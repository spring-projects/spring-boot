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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

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
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.micrometer.tracing.autoconfigure.ConditionalOnEnabledTracingExport;
import org.springframework.boot.opentelemetry.autoconfigure.OtlpProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
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
		@Conditional(OtlpEndpointCondition.class)
		OtlpTracingConnectionDetails otlpTracingConnectionDetails(OtlpTracingProperties properties,
				OtlpProperties otlpProperties, ObjectProvider<SslBundles> sslBundles) {
			return new PropertiesOtlpTracingConnectionDetails(properties, otlpProperties, sslBundles.getIfAvailable());
		}

		/**
		 * Condition to check if either the tracing-specific endpoint or the common OTLP
		 * endpoint is set.
		 */
		static class OtlpEndpointCondition extends AnyNestedCondition {

			OtlpEndpointCondition() {
				super(ConfigurationPhase.REGISTER_BEAN);
			}

			@ConditionalOnProperty("management.opentelemetry.tracing.export.otlp.endpoint")
			@SuppressWarnings("unused")
			static class TracingEndpoint {

			}

			@ConditionalOnProperty("management.opentelemetry.otlp.endpoint")
			@SuppressWarnings("unused")
			static class CommonEndpoint {

			}

		}

		/**
		 * Adapts {@link OtlpTracingProperties} and {@link OtlpProperties} to
		 * {@link OtlpTracingConnectionDetails}.
		 */
		static class PropertiesOtlpTracingConnectionDetails implements OtlpTracingConnectionDetails {

			private final OtlpTracingProperties properties;

			private final OtlpProperties otlpProperties;

			private final @Nullable SslBundles sslBundles;

			PropertiesOtlpTracingConnectionDetails(OtlpTracingProperties properties, OtlpProperties otlpProperties,
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
						endpoint = endpoint.endsWith("/") ? endpoint + "v1/traces" : endpoint + "/v1/traces";
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
	@ConditionalOnMissingBean({ OtlpGrpcSpanExporter.class, OtlpHttpSpanExporter.class })
	@ConditionalOnBean(OtlpTracingConnectionDetails.class)
	@ConditionalOnEnabledTracingExport("otlp")
	static class Exporters {

		@Bean
		@ConditionalOnProperty(name = "management.opentelemetry.tracing.export.otlp.transport", havingValue = "http",
				matchIfMissing = true)
		OtlpHttpSpanExporter otlpHttpSpanExporter(OtlpTracingProperties properties, OtlpProperties otlpProperties,
				OtlpTracingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider,
				ObjectProvider<OtlpHttpSpanExporterBuilderCustomizer> customizers) {
			OtlpHttpSpanExporterBuilder builder = OtlpHttpSpanExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.HTTP));

			Duration timeout = properties.getTimeout();
			builder.setTimeout(timeout);

			Duration connectTimeout = properties.getConnectTimeout();
			builder.setConnectTimeout(connectTimeout);

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
		@ConditionalOnProperty(name = "management.opentelemetry.tracing.export.otlp.transport", havingValue = "grpc")
		OtlpGrpcSpanExporter otlpGrpcSpanExporter(OtlpTracingProperties properties, OtlpProperties otlpProperties,
				OtlpTracingConnectionDetails connectionDetails, ObjectProvider<MeterProvider> meterProvider,
				ObjectProvider<OtlpGrpcSpanExporterBuilderCustomizer> customizers) {
			OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder()
				.setEndpoint(connectionDetails.getUrl(Transport.GRPC));

			Duration timeout = properties.getTimeout();
			builder.setTimeout(timeout);

			Duration connectTimeout = properties.getConnectTimeout();
			builder.setConnectTimeout(connectTimeout);

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

		static class HttpTransportCondition extends SpringBootCondition {

			@Override
			public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
				String tracingTransport = context.getEnvironment()
					.getProperty("management.opentelemetry.tracing.export.otlp.transport");
				String activeTransport = (tracingTransport != null) ? tracingTransport : "http";
				return new ConditionOutcome("http".equalsIgnoreCase(activeTransport),
						"Transport is " + activeTransport);
			}

		}

		static class GrpcTransportCondition extends SpringBootCondition {

			@Override
			public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
				String tracingTransport = context.getEnvironment()
					.getProperty("management.opentelemetry.tracing.export.otlp.transport");
				String activeTransport = (tracingTransport != null) ? tracingTransport : "http";
				return new ConditionOutcome("grpc".equalsIgnoreCase(activeTransport),
						"Transport is " + activeTransport);
			}

		}

	}

}
