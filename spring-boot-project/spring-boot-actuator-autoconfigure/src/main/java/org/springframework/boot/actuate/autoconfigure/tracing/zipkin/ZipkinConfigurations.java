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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.tracing.ConditionalOnEnabledTracing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configurations for Zipkin. Those are imported by {@link ZipkinAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 */
class ZipkinConfigurations {

	@Configuration(proxyBeanMethods = false)
	@Import({ UrlConnectionSenderConfiguration.class, WebClientSenderConfiguration.class,
			RestTemplateSenderConfiguration.class })
	static class SenderConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(URLConnectionSender.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class UrlConnectionSenderConfiguration {

		@Bean
		@ConditionalOnMissingBean(Sender.class)
		URLConnectionSender urlConnectionSender(ZipkinProperties properties,
				ObjectProvider<ZipkinConnectionDetails> connectionDetailsProvider) {
			ZipkinConnectionDetails connectionDetails = connectionDetailsProvider
				.getIfAvailable(() -> new PropertiesZipkinConnectionDetails(properties));
			URLConnectionSender.Builder builder = URLConnectionSender.newBuilder();
			builder.connectTimeout((int) properties.getConnectTimeout().toMillis());
			builder.readTimeout((int) properties.getReadTimeout().toMillis());
			builder.endpoint(connectionDetails.getSpanEndpoint());
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RestTemplate.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class RestTemplateSenderConfiguration {

		@Bean
		@ConditionalOnMissingBean(Sender.class)
		ZipkinRestTemplateSender restTemplateSender(ZipkinProperties properties,
				ObjectProvider<ZipkinRestTemplateBuilderCustomizer> customizers,
				ObjectProvider<ZipkinConnectionDetails> connectionDetailsProvider) {
			ZipkinConnectionDetails connectionDetails = connectionDetailsProvider
				.getIfAvailable(() -> new PropertiesZipkinConnectionDetails(properties));
			RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder()
				.setConnectTimeout(properties.getConnectTimeout())
				.setReadTimeout(properties.getReadTimeout());
			restTemplateBuilder = applyCustomizers(restTemplateBuilder, customizers);
			return new ZipkinRestTemplateSender(connectionDetails.getSpanEndpoint(), restTemplateBuilder.build());
		}

		private RestTemplateBuilder applyCustomizers(RestTemplateBuilder restTemplateBuilder,
				ObjectProvider<ZipkinRestTemplateBuilderCustomizer> customizers) {
			Iterable<ZipkinRestTemplateBuilderCustomizer> orderedCustomizers = () -> customizers.orderedStream()
				.iterator();
			RestTemplateBuilder currentBuilder = restTemplateBuilder;
			for (ZipkinRestTemplateBuilderCustomizer customizer : orderedCustomizers) {
				currentBuilder = customizer.customize(currentBuilder);
			}
			return currentBuilder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebClient.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class WebClientSenderConfiguration {

		@Bean
		@ConditionalOnMissingBean(Sender.class)
		ZipkinWebClientSender webClientSender(ZipkinProperties properties,
				ObjectProvider<ZipkinWebClientBuilderCustomizer> customizers,
				ObjectProvider<ZipkinConnectionDetails> connectionDetailsProvider) {
			ZipkinConnectionDetails connectionDetails = connectionDetailsProvider
				.getIfAvailable(() -> new PropertiesZipkinConnectionDetails(properties));
			WebClient.Builder builder = WebClient.builder();
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return new ZipkinWebClientSender(connectionDetails.getSpanEndpoint(), builder.build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReporterConfiguration {

		@Bean
		@ConditionalOnMissingBean(Reporter.class)
		@ConditionalOnBean(Sender.class)
		AsyncReporter<Span> spanReporter(Sender sender, BytesEncoder<Span> encoder) {
			return AsyncReporter.builder(sender).build(encoder);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ZipkinSpanHandler.class)
	static class BraveConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(Reporter.class)
		@ConditionalOnEnabledTracing
		ZipkinSpanHandler zipkinSpanHandler(Reporter<Span> spanReporter) {
			return (ZipkinSpanHandler) ZipkinSpanHandler.newBuilder(spanReporter).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ZipkinSpanExporter.class)
	static class OpenTelemetryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(Sender.class)
		@ConditionalOnEnabledTracing
		ZipkinSpanExporter zipkinSpanExporter(BytesEncoder<Span> encoder, Sender sender) {
			return ZipkinSpanExporter.builder().setEncoder(encoder).setSender(sender).build();
		}

	}

}
