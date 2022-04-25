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

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.opentelemetry.exporter.zipkin.ZipkinSpanExporter;
import reactor.netty.http.client.HttpClient;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configurations for Zipkin. Those are imported by {@link ZipkinAutoConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ZipkinConfigurations {

	@Configuration(proxyBeanMethods = false)
	@Import({ UrlConnectionSenderConfiguration.class, RestTemplateSenderConfiguration.class })
	static class SenderConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(URLConnectionSender.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class UrlConnectionSenderConfiguration {

		@Bean
		@ConditionalOnMissingBean(Sender.class)
		URLConnectionSender urlConnectionSender(ZipkinProperties properties) {
			return URLConnectionSender.newBuilder().connectTimeout((int) properties.getConnectTimeout().getSeconds())
					.readTimeout((int) properties.getReadTimeout().getSeconds()).endpoint(properties.getEndpoint())
					.build();
		}

	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	@ConditionalOnBean(WebClient.Builder.class)
	@ConditionalOnMissingClass("zipkin2.reporter.urlconnection.URLConnectionSender")
	Sender webClientSender(ZipkinProperties properties, WebClient.Builder webClientBuilder) {
		HttpClient client = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.getConnectTimeout().toMillis())
				.doOnConnected((conn) -> conn
						.addHandlerLast(new ReadTimeoutHandler((int) properties.getReadTimeout().toSeconds())));
		WebClient webClient = webClientBuilder.clientConnector(new ReactorClientHttpConnector(client)).build();
		return new ZipkinWebClientSender(properties.getEndpoint(), webClient);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RestTemplate.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class RestTemplateSenderConfiguration {

		@Bean
		@ConditionalOnMissingBean(Sender.class)
		@ConditionalOnBean(RestTemplateBuilder.class)
		ZipkinRestTemplateSender restTemplateSender(ZipkinProperties properties,
				RestTemplateBuilder restTemplateBuilder) {
			RestTemplate restTemplate = restTemplateBuilder.setConnectTimeout(properties.getConnectTimeout())
					.setReadTimeout(properties.getReadTimeout()).build();
			return new ZipkinRestTemplateSender(properties.getEndpoint(), restTemplate);
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
