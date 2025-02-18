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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;

import brave.Tag;
import brave.Tags;
import brave.handler.MutableSpan;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import zipkin2.Span;
import zipkin2.reporter.BytesEncoder;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;
import zipkin2.reporter.HttpEndpointSupplier;
import zipkin2.reporter.HttpEndpointSuppliers;
import zipkin2.reporter.SpanBytesEncoder;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.brave.MutableSpanBytesEncoder;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configurations for Zipkin. Those are imported by {@link ZipkinAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 * @author Wick Dynex
 */
class ZipkinConfigurations {

	@Configuration(proxyBeanMethods = false)
	@Import({ HttpClientSenderConfiguration.class, UrlConnectionSenderConfiguration.class })
	static class SenderConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HttpClient.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class HttpClientSenderConfiguration {

		@Bean
		@ConditionalOnMissingBean(BytesMessageSender.class)
		ZipkinHttpClientSender httpClientSender(ZipkinProperties properties, Encoding encoding,
				ObjectProvider<ZipkinHttpClientBuilderCustomizer> customizers,
				ObjectProvider<ZipkinConnectionDetails> connectionDetailsProvider,
				ObjectProvider<HttpEndpointSupplier.Factory> endpointSupplierFactoryProvider) {
			ZipkinConnectionDetails connectionDetails = connectionDetailsProvider
				.getIfAvailable(() -> new PropertiesZipkinConnectionDetails(properties));
			HttpEndpointSupplier.Factory endpointSupplierFactory = endpointSupplierFactoryProvider
				.getIfAvailable(HttpEndpointSuppliers::constantFactory);
			Builder httpClientBuilder = HttpClient.newBuilder().connectTimeout(properties.getConnectTimeout());
			customizers.orderedStream().forEach((customizer) -> customizer.customize(httpClientBuilder));
			return new ZipkinHttpClientSender(encoding, endpointSupplierFactory, connectionDetails.getSpanEndpoint(),
					httpClientBuilder.build(), properties.getReadTimeout());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(URLConnectionSender.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class UrlConnectionSenderConfiguration {

		@Bean
		@ConditionalOnMissingBean(BytesMessageSender.class)
		URLConnectionSender urlConnectionSender(ZipkinProperties properties, Encoding encoding,
				ObjectProvider<ZipkinConnectionDetails> connectionDetailsProvider,
				ObjectProvider<HttpEndpointSupplier.Factory> endpointSupplierFactoryProvider) {
			ZipkinConnectionDetails connectionDetails = connectionDetailsProvider
				.getIfAvailable(() -> new PropertiesZipkinConnectionDetails(properties));
			HttpEndpointSupplier.Factory endpointSupplierFactory = endpointSupplierFactoryProvider
				.getIfAvailable(HttpEndpointSuppliers::constantFactory);
			URLConnectionSender.Builder builder = URLConnectionSender.newBuilder();
			builder.connectTimeout((int) properties.getConnectTimeout().toMillis());
			builder.readTimeout((int) properties.getReadTimeout().toMillis());
			builder.endpointSupplierFactory(endpointSupplierFactory);
			builder.endpoint(connectionDetails.getSpanEndpoint());
			builder.encoding(encoding);
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(AsyncZipkinSpanHandler.class)
	static class BraveConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = MutableSpan.class, parameterizedContainer = BytesEncoder.class)
		BytesEncoder<MutableSpan> mutableSpanBytesEncoder(Encoding encoding,
				ObjectProvider<Tag<Throwable>> throwableTagProvider) {
			Tag<Throwable> throwableTag = throwableTagProvider.getIfAvailable(() -> Tags.ERROR);
			return MutableSpanBytesEncoder.create(encoding, throwableTag);
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(BytesMessageSender.class)
		@ConditionalOnEnabledTracing("zipkin")
		AsyncZipkinSpanHandler asyncZipkinSpanHandler(BytesMessageSender sender,
				BytesEncoder<MutableSpan> mutableSpanBytesEncoder) {
			return AsyncZipkinSpanHandler.newBuilder(sender).build(mutableSpanBytesEncoder);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ ZipkinSpanExporter.class, Span.class })
	static class OpenTelemetryConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = Span.class, parameterizedContainer = BytesEncoder.class)
		BytesEncoder<Span> spanBytesEncoder(Encoding encoding) {
			return SpanBytesEncoder.forEncoding(encoding);
		}

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(BytesMessageSender.class)
		@ConditionalOnEnabledTracing("zipkin")
		ZipkinSpanExporter zipkinSpanExporter(BytesMessageSender sender, BytesEncoder<Span> spanBytesEncoder) {
			return ZipkinSpanExporter.builder().setSender(sender).setEncoder(spanBytesEncoder).build();
		}

	}

}
