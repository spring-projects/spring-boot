/*
 * Copyright 2012-2022 the original author or authors.
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
		URLConnectionSender urlConnectionSender(ZipkinProperties properties) {
			return URLConnectionSender.newBuilder().connectTimeout((int) properties.getConnectTimeout().toMillis())
					.readTimeout((int) properties.getReadTimeout().toMillis()).endpoint(properties.getEndpoint())
					.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RestTemplate.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class RestTemplateSenderConfiguration {

		@Bean
		@ConditionalOnMissingBean(Sender.class)
		ZipkinRestTemplateSender restTemplateSender(ZipkinProperties properties,
				ObjectProvider<ZipkinRestTemplateBuilderCustomizer> customizers) {
			RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder()
					.setConnectTimeout(properties.getConnectTimeout()).setReadTimeout(properties.getReadTimeout());
			customizers.orderedStream().forEach((c) -> c.customize(restTemplateBuilder));
			return new ZipkinRestTemplateSender(properties.getEndpoint(), restTemplateBuilder.build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebClient.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class WebClientSenderConfiguration {

		@Bean
		@ConditionalOnMissingBean(Sender.class)
		ZipkinWebClientSender webClientSender(ZipkinProperties properties,
				ObjectProvider<ZipkinWebClientBuilderCustomizer> customizers) {
			WebClient.Builder builder = WebClient.builder();
			customizers.orderedStream().forEach((c) -> c.customize(builder));
			return new ZipkinWebClientSender(properties.getEndpoint(), builder.build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReporterConfiguration {

		@Bean
		@ConditionalOnMissingBean
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
		ZipkinSpanExporter zipkinSpanExporter(BytesEncoder<Span> encoder, Sender sender) {
			return ZipkinSpanExporter.builder().setEncoder(encoder).setSender(sender).build();
		}

	}

}
