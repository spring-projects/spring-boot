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

	/**
     * SenderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@Import({ UrlConnectionSenderConfiguration.class, WebClientSenderConfiguration.class,
			RestTemplateSenderConfiguration.class })
	static class SenderConfiguration {

	}

	/**
     * UrlConnectionSenderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(URLConnectionSender.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class UrlConnectionSenderConfiguration {

		/**
         * Creates a new instance of URLConnectionSender if no other bean of type Sender is available.
         * Uses the provided ZipkinProperties and ZipkinConnectionDetails to configure the sender.
         * Sets the connect timeout and read timeout based on the properties.
         * Sets the endpoint based on the connection details.
         * 
         * @param properties The ZipkinProperties used to configure the sender.
         * @param connectionDetailsProvider The provider for ZipkinConnectionDetails.
         * @return The created URLConnectionSender.
         */
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

	/**
     * RestTemplateSenderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RestTemplate.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class RestTemplateSenderConfiguration {

		/**
         * Creates a {@link ZipkinRestTemplateSender} bean if no other bean of type {@link Sender} is present.
         * 
         * @param properties the {@link ZipkinProperties} containing the configuration properties for Zipkin
         * @param customizers the {@link ZipkinRestTemplateBuilderCustomizer} objects to customize the {@link RestTemplateBuilder}
         * @param connectionDetailsProvider the {@link ZipkinConnectionDetails} provider to get the connection details for Zipkin
         * @return the {@link ZipkinRestTemplateSender} bean
         */
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

		/**
         * Applies customizers to the given RestTemplateBuilder.
         * 
         * @param restTemplateBuilder The RestTemplateBuilder to apply customizers to.
         * @param customizers         ObjectProvider of ZipkinRestTemplateBuilderCustomizer to provide customizers.
         * @return The RestTemplateBuilder with customizers applied.
         */
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

	/**
     * WebClientSenderConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebClient.class)
	@EnableConfigurationProperties(ZipkinProperties.class)
	static class WebClientSenderConfiguration {

		/**
         * Creates a ZipkinWebClientSender bean if no other bean of type Sender is present.
         * Uses the provided ZipkinProperties, ZipkinWebClientBuilderCustomizer, and ZipkinConnectionDetails
         * to configure and create a ZipkinWebClientSender instance.
         * If no ZipkinConnectionDetails bean is available, it creates a new instance using the provided ZipkinProperties.
         * Uses the WebClient.Builder to build a WebClient instance with customizations from the ZipkinWebClientBuilderCustomizer beans.
         * Returns the created ZipkinWebClientSender with the configured span endpoint and timeouts.
         *
         * @param properties                the ZipkinProperties used for configuration
         * @param customizers               the ZipkinWebClientBuilderCustomizer beans for customizing the WebClient.Builder
         * @param connectionDetailsProvider the ZipkinConnectionDetails bean provider for retrieving the connection details
         * @return the created ZipkinWebClientSender
         */
        @Bean
		@ConditionalOnMissingBean(Sender.class)
		ZipkinWebClientSender webClientSender(ZipkinProperties properties,
				ObjectProvider<ZipkinWebClientBuilderCustomizer> customizers,
				ObjectProvider<ZipkinConnectionDetails> connectionDetailsProvider) {
			ZipkinConnectionDetails connectionDetails = connectionDetailsProvider
				.getIfAvailable(() -> new PropertiesZipkinConnectionDetails(properties));
			WebClient.Builder builder = WebClient.builder();
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return new ZipkinWebClientSender(connectionDetails.getSpanEndpoint(), builder.build(),
					properties.getConnectTimeout().plus(properties.getReadTimeout()));
		}

	}

	/**
     * ReporterConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	static class ReporterConfiguration {

		/**
         * Creates an asynchronous reporter for spans.
         * 
         * This method is annotated with @Bean to indicate that it is a Spring bean.
         * 
         * It is also annotated with @ConditionalOnMissingBean(Reporter.class) to ensure that this bean is only created if there is no other bean of type Reporter already present in the application context.
         * 
         * Additionally, it is annotated with @ConditionalOnBean(Sender.class) to ensure that this bean is only created if there is a bean of type Sender already present in the application context.
         * 
         * The method takes a Sender object and a BytesEncoder object as parameters.
         * 
         * It returns an AsyncReporter object that is built using the provided Sender and BytesEncoder objects.
         * 
         * @param sender The Sender object used for sending spans.
         * @param encoder The BytesEncoder object used for encoding spans.
         * @return An AsyncReporter object for spans.
         */
        @Bean
		@ConditionalOnMissingBean(Reporter.class)
		@ConditionalOnBean(Sender.class)
		AsyncReporter<Span> spanReporter(Sender sender, BytesEncoder<Span> encoder) {
			return AsyncReporter.builder(sender).build(encoder);
		}

	}

	/**
     * BraveConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ZipkinSpanHandler.class)
	static class BraveConfiguration {

		/**
         * Creates a ZipkinSpanHandler bean if there is no existing bean of the same type, a bean of type Reporter<Span> exists,
         * and tracing is enabled.
         *
         * @param spanReporter the reporter used to report spans
         * @return the created ZipkinSpanHandler bean
         */
        @Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(Reporter.class)
		@ConditionalOnEnabledTracing
		ZipkinSpanHandler zipkinSpanHandler(Reporter<Span> spanReporter) {
			return (ZipkinSpanHandler) ZipkinSpanHandler.newBuilder(spanReporter).build();
		}

	}

	/**
     * OpenTelemetryConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ZipkinSpanExporter.class)
	static class OpenTelemetryConfiguration {

		/**
         * Creates a ZipkinSpanExporter with the provided encoder and sender.
         * 
         * @param encoder the encoder used to encode spans into bytes
         * @param sender the sender used to send the encoded spans
         * @return a ZipkinSpanExporter instance
         * @throws IllegalArgumentException if either encoder or sender is null
         * @see BytesEncoder
         * @see Sender
         * @see ZipkinSpanExporter
         */
        @Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(Sender.class)
		@ConditionalOnEnabledTracing
		ZipkinSpanExporter zipkinSpanExporter(BytesEncoder<Span> encoder, Sender sender) {
			return ZipkinSpanExporter.builder().setEncoder(encoder).setSender(sender).build();
		}

	}

}
