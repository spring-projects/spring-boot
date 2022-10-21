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
				.withClassLoader(new FilteredClassLoader("zipkin2.reporter.urlconnection")).run((context) -> {
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
				.withClassLoader(new FilteredClassLoader("zipkin2.reporter.urlconnection")).run((context) -> {
					assertThat(context).doesNotHaveBean(URLConnectionSender.class);
					assertThat(context).hasSingleBean(Sender.class);
					assertThat(context).hasSingleBean(ZipkinWebClientSender.class);
				});
	}

	@Test
	void shouldPreferWebClientInNonWebApplicationAndUrlConnectionSenderIsNotAvailable() {
		this.contextRunner.withUserConfiguration(RestTemplateConfiguration.class, WebClientConfiguration.class)
				.withClassLoader(new FilteredClassLoader("zipkin2.reporter.urlconnection")).run((context) -> {
					assertThat(context).doesNotHaveBean(URLConnectionSender.class);
					assertThat(context).hasSingleBean(Sender.class);
					assertThat(context).hasSingleBean(ZipkinWebClientSender.class);
				});
	}

	@Test
	void willUseRestTemplateInNonWebApplicationIfUrlConnectionSenderAndWebClientAreNotAvailable() {
		this.contextRunner.withUserConfiguration(RestTemplateConfiguration.class)
				.withClassLoader(new FilteredClassLoader(URLConnectionSender.class, WebClient.class)).run((context) -> {
					assertThat(context).doesNotHaveBean(URLConnectionSender.class);
					assertThat(context).hasSingleBean(Sender.class);
					assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
				});
	}

	@Test
	void willUseRestTemplateInServletWebApplicationIfUrlConnectionSenderAndWebClientNotAvailable() {
		this.servletContextRunner.withUserConfiguration(RestTemplateConfiguration.class)
				.withClassLoader(new FilteredClassLoader(URLConnectionSender.class, WebClient.class)).run((context) -> {
					assertThat(context).doesNotHaveBean(URLConnectionSender.class);
					assertThat(context).hasSingleBean(Sender.class);
					assertThat(context).hasSingleBean(ZipkinRestTemplateSender.class);
				});
	}

	@Test
	void willUseRestTemplateInReactiveWebApplicationIfUrlConnectionSenderAndWebClientAreNotAvailable() {
		this.reactiveContextRunner.withUserConfiguration(RestTemplateConfiguration.class)
				.withClassLoader(new FilteredClassLoader(URLConnectionSender.class, WebClient.class)).run((context) -> {
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

	@Configuration(proxyBeanMethods = false)
	private static class RestTemplateConfiguration {

		@Bean
		ZipkinRestTemplateBuilderCustomizer restTemplateBuilder() {
			return mock(ZipkinRestTemplateBuilderCustomizer.class);
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

}
