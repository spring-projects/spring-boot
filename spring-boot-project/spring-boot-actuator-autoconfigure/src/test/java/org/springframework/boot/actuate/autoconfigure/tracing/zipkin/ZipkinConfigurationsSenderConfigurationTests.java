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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import zipkin2.reporter.Sender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConfigurations.SenderConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SenderConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ZipkinConfigurationsSenderConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SenderConfiguration.class));

	private final ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SenderConfiguration.class));

	private final WebApplicationContextRunner servletContextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(SenderConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Sender.class);
			assertThat(context).hasSingleBean(URLConnectionSender.class);
			assertThat(context).doesNotHaveBean(ZipkinRestTemplateSender.class);
		});
	}

	@Test
	void shouldPreferWebClientSenderIfWebApplicationIsReactiveAndUrlSenderIsNotAvailable() {
		this.reactiveContextRunner.withUserConfiguration(RestTemplateConfiguration.class, WebClientConfiguration.class)
			.withClassLoader(new FilteredClassLoader("zipkin2.reporter.urlconnection"))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(URLConnectionSender.class);
				assertThat(context).hasSingleBean(Sender.class);
				assertThat(context).hasSingleBean(ZipkinWebClientSender.class);
				then(context.getBean(ZipkinWebClientBuilderCustomizer.class)).should()
					.customize(ArgumentMatchers.any());
			});
	}

	@Test
	void shouldPreferWebClientSenderIfWebApplicationIsServletAndUrlSenderIsNotAvailable() {
		this.servletContextRunner.withUserConfiguration(RestTemplateConfiguration.class, WebClientConfiguration.class)
			.withClassLoader(new FilteredClassLoader("zipkin2.reporter.urlconnection"))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(URLConnectionSender.class);
				assertThat(context).hasSingleBean(Sender.class);
				assertThat(context).hasSingleBean(ZipkinWebClientSender.class);
			});
	}

	@Test
	void shouldPreferWebClientInNonWebApplicationAndUrlConnectionSenderIsNotAvailable() {
		this.contextRunner.withUserConfiguration(RestTemplateConfiguration.class, WebClientConfiguration.class)
			.withClassLoader(new FilteredClassLoader("zipkin2.reporter.urlconnection"))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(URLConnectionSender.class);
				assertThat(context).hasSingleBean(Sender.class);
				assertThat(context).hasSingleBean(ZipkinWebClientSender.class);
			});
	}

	@Test
	void willUseRestTemplateInNonWebApplicationIfUrlConnectionSenderAndWebClientAreNotAvailable() {
		this.contextRunner.withUserConfiguration(RestTemplateConfiguration.class)
			.withClassLoader(new FilteredClassLoader(URLConnectionSender.class, WebClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(URLConnectionSender.class);
				assertThat(context).hasSingleBean(Sender.class);
				assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
			});
	}

	@Test
	void willUseRestTemplateInServletWebApplicationIfUrlConnectionSenderAndWebClientNotAvailable() {
		this.servletContextRunner.withUserConfiguration(RestTemplateConfiguration.class)
			.withClassLoader(new FilteredClassLoader(URLConnectionSender.class, WebClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(URLConnectionSender.class);
				assertThat(context).hasSingleBean(Sender.class);
				assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
			});
	}

	@Test
	void willUseRestTemplateInReactiveWebApplicationIfUrlConnectionSenderAndWebClientAreNotAvailable() {
		this.reactiveContextRunner.withUserConfiguration(RestTemplateConfiguration.class)
			.withClassLoader(new FilteredClassLoader(URLConnectionSender.class, WebClient.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(URLConnectionSender.class);
				assertThat(context).hasSingleBean(Sender.class);
				assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
			});
	}

	@Test
	void shouldNotUseWebClientSenderIfNoBuilderIsAvailable() {
		this.reactiveContextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(ZipkinWebClientSender.class);
			assertThat(context).hasSingleBean(Sender.class);
			assertThat(context).hasSingleBean(URLConnectionSender.class);
		});
	}

	@Test
	void shouldBackOffOnCustomBeans() {
		this.contextRunner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			assertThat(context).hasBean("customSender");
			assertThat(context).hasSingleBean(Sender.class);
		});
	}

	@Test
	void shouldApplyZipkinRestTemplateBuilderCustomizers() throws IOException {
		try (MockWebServer mockWebServer = new MockWebServer()) {
			mockWebServer.enqueue(new MockResponse().setResponseCode(204));
			this.reactiveContextRunner
				.withPropertyValues("management.zipkin.tracing.endpoint=" + mockWebServer.url("/"))
				.withUserConfiguration(RestTemplateConfiguration.class)
				.withClassLoader(new FilteredClassLoader(URLConnectionSender.class, WebClient.class))
				.run((context) -> {
					assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
					ZipkinRestTemplateSender sender = context.getBean(ZipkinRestTemplateSender.class);
					sender.sendSpans("spans".getBytes(StandardCharsets.UTF_8)).execute();
					RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
					assertThat(recordedRequest).isNotNull();
					assertThat(recordedRequest.getHeaders().get("x-dummy")).isEqualTo("dummy");
				});
		}
	}

	@Configuration(proxyBeanMethods = false)
	private static class RestTemplateConfiguration {

		@Bean
		ZipkinRestTemplateBuilderCustomizer zipkinRestTemplateBuilderCustomizer() {
			return new DummyZipkinRestTemplateBuilderCustomizer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class WebClientConfiguration {

		@Bean
		ZipkinWebClientBuilderCustomizer webClientBuilder() {
			return mock(ZipkinWebClientBuilderCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	private static class CustomConfiguration {

		@Bean
		Sender customSender() {
			return mock(Sender.class);
		}

	}

	private static class DummyZipkinRestTemplateBuilderCustomizer implements ZipkinRestTemplateBuilderCustomizer {

		@Override
		public RestTemplateBuilder customize(RestTemplateBuilder restTemplateBuilder) {
			return restTemplateBuilder.defaultHeader("x-dummy", "dummy");
		}

	}

}
