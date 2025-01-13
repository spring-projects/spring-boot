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

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.HttpEndpointSupplier;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.SenderConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.UrlConnectionSenderConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SenderConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Wick Dynex
 */
@SuppressWarnings({ "deprecation", "removal" })
class ZipkinConfigurationsSenderConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DefaultEncodingConfiguration.class, SenderConfiguration.class));

	private final ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DefaultEncodingConfiguration.class, SenderConfiguration.class));

	private final WebApplicationContextRunner servletContextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DefaultEncodingConfiguration.class, SenderConfiguration.class));

	@Test
	void shouldSupplyDefaultHttpClientSenderBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(BytesMessageSender.class);
			assertThat(context).hasSingleBean(ZipkinHttpClientSender.class);
			assertThat(context).doesNotHaveBean(ZipkinRestTemplateSender.class);
			assertThat(context).doesNotHaveBean(ZipkinWebClientSenderTests.class);
			assertThat(context).doesNotHaveBean(URLConnectionSender.class);
		});
	}

	@Test
	void shouldUseUrlSenderIfHttpSenderIsNotAvailable() {
		this.contextRunner.withUserConfiguration(UrlConnectionSenderConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class, WebClient.class, RestTemplate.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ZipkinHttpClientSender.class);
				assertThat(context).hasSingleBean(BytesMessageSender.class);
				assertThat(context).hasSingleBean(URLConnectionSender.class);
			});
	}

	@Test
	void shouldPreferWebClientSenderIfWebApplicationIsReactiveAndHttpClientSenderIsNotAvailable() {
		this.reactiveContextRunner.withUserConfiguration(RestTemplateConfiguration.class, WebClientConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ZipkinHttpClientSender.class);
				assertThat(context).hasSingleBean(BytesMessageSender.class);
				assertThat(context).hasSingleBean(ZipkinWebClientSender.class);
				then(context.getBean(ZipkinWebClientBuilderCustomizer.class)).should()
					.customize(ArgumentMatchers.any());
			});
	}

	@Test
	void shouldPreferWebClientSenderIfWebApplicationIsServletAndHttpClientSenderIsNotAvailable() {
		this.servletContextRunner.withUserConfiguration(RestTemplateConfiguration.class, WebClientConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ZipkinHttpClientSender.class);
				assertThat(context).hasSingleBean(BytesMessageSender.class);
				assertThat(context).hasSingleBean(ZipkinWebClientSender.class);
			});
	}

	@Test
	void shouldPreferWebClientInNonWebApplicationAndHttpClientSenderIsNotAvailable() {
		this.contextRunner.withUserConfiguration(RestTemplateConfiguration.class, WebClientConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ZipkinHttpClientSender.class);
				assertThat(context).hasSingleBean(BytesMessageSender.class);
				assertThat(context).hasSingleBean(ZipkinWebClientSender.class);
			});
	}

	@Test
	void willUseRestTemplateInNonWebApplicationIfSenderAndWebClientAreNotAvailable() {
		this.contextRunner.withUserConfiguration(RestTemplateConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class, WebClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(HttpClient.class);
				assertThat(context).hasSingleBean(BytesMessageSender.class);
				assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
			});
	}

	@Test
	void willUseRestTemplateInServletWebApplicationIfHttpClientSenderAndWebClientNotAvailable() {
		this.servletContextRunner.withUserConfiguration(RestTemplateConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class, WebClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ZipkinHttpClientSender.class);
				assertThat(context).hasSingleBean(BytesMessageSender.class);
				assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
			});
	}

	@Test
	void willUseRestTemplateInReactiveWebApplicationIfHttpClientSenderAndWebClientAreNotAvailable() {
		this.reactiveContextRunner.withUserConfiguration(RestTemplateConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class, WebClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(ZipkinHttpClientSender.class);
				assertThat(context).hasSingleBean(BytesMessageSender.class);
				assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
			});
	}

	@Test
	void shouldNotUseWebClientSenderIfNoBuilderIsAvailable() {
		this.reactiveContextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(ZipkinWebClientSender.class);
			assertThat(context).hasSingleBean(BytesMessageSender.class);
			assertThat(context).hasSingleBean(ZipkinHttpClientSender.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customSender");
			assertThat(context).hasSingleBean(BytesMessageSender.class);
		});
	}

	@Test
	void shouldApplyZipkinRestTemplateBuilderCustomizers() throws IOException {
		try (MockWebServer mockWebServer = new MockWebServer()) {
			mockWebServer.enqueue(new MockResponse().setResponseCode(204));
			this.reactiveContextRunner
				.withPropertyValues("management.zipkin.tracing.endpoint=" + mockWebServer.url("/"))
				.withUserConfiguration(RestTemplateConfiguration.class)
				.withClassLoader(new FilteredClassLoader(HttpClient.class, WebClient.class))
				.run((context) -> {
					assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
					ZipkinRestTemplateSender sender = context.getBean(ZipkinRestTemplateSender.class);
					sender.send(List.of("spans".getBytes(StandardCharsets.UTF_8)));
					RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
					assertThat(recordedRequest).isNotNull();
					assertThat(recordedRequest.getHeaders().get("x-dummy")).isEqualTo("dummy");
				});
		}
	}

	@Test
	void shouldUseCustomHttpEndpointSupplierFactory() {
		this.contextRunner.withUserConfiguration(CustomHttpEndpointSupplierFactoryConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class, WebClient.class, RestTemplate.class))
			.run((context) -> assertThat(context.getBean(URLConnectionSender.class))
				.extracting("delegate.endpointSupplier")
				.isInstanceOf(CustomHttpEndpointSupplier.class));
	}

	@Test
	@SuppressWarnings("resource")
	void shouldUseCustomHttpEndpointSupplierFactoryWhenReactive() {
		this.reactiveContextRunner.withUserConfiguration(WebClientConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class))
			.withUserConfiguration(CustomHttpEndpointSupplierFactoryConfiguration.class)
			.run((context) -> assertThat(context.getBean(ZipkinWebClientSender.class)).extracting("endpointSupplier")
				.isInstanceOf(CustomHttpEndpointSupplier.class));
	}

	@Test
	@SuppressWarnings("resource")
	void shouldUseCustomHttpEndpointSupplierFactoryWhenRestTemplate() {
		this.contextRunner.withUserConfiguration(RestTemplateConfiguration.class)
			.withClassLoader(new FilteredClassLoader(HttpClient.class, WebClient.class))
			.withUserConfiguration(CustomHttpEndpointSupplierFactoryConfiguration.class)
			.run((context) -> assertThat(context.getBean(ZipkinRestTemplateSender.class)).extracting("endpointSupplier")
				.isInstanceOf(CustomHttpEndpointSupplier.class));
	}

	@Configuration(proxyBeanMethods = false)
	private static final class RestTemplateConfiguration {

		@Bean
		ZipkinRestTemplateBuilderCustomizer zipkinRestTemplateBuilderCustomizer() {
			return new DummyZipkinRestTemplateBuilderCustomizer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class WebClientConfiguration {

		@Bean
		ZipkinWebClientBuilderCustomizer webClientBuilder() {
			return mock(ZipkinWebClientBuilderCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class HttpClientConfiguration {

		@Bean
		ZipkinHttpClientBuilderCustomizer httpClientBuilderCustomizer() {
			return mock(ZipkinHttpClientBuilderCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomConfiguration {

		@Bean
		BytesMessageSender customSender() {
			return mock(BytesMessageSender.class);
		}

	}

	private static final class DummyZipkinRestTemplateBuilderCustomizer implements ZipkinRestTemplateBuilderCustomizer {

		@Override
		public RestTemplateBuilder customize(RestTemplateBuilder restTemplateBuilder) {
			return restTemplateBuilder.defaultHeader("x-dummy", "dummy");
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static final class CustomHttpEndpointSupplierFactoryConfiguration {

		@Bean
		HttpEndpointSupplier.Factory httpEndpointSupplier() {
			return new CustomHttpEndpointSupplierFactory();
		}

	}

	private static final class CustomHttpEndpointSupplierFactory implements HttpEndpointSupplier.Factory {

		@Override
		public HttpEndpointSupplier create(String endpoint) {
			return new CustomHttpEndpointSupplier(endpoint);
		}

	}

	private record CustomHttpEndpointSupplier(String endpoint) implements HttpEndpointSupplier {

		@Override
		public String get() {
			return this.endpoint;
		}

		@Override
		public void close() {
		}
	}

}
