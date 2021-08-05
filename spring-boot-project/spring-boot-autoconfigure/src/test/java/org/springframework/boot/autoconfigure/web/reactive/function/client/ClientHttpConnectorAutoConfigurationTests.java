/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ClientHttpConnectorAutoConfiguration}
 *
 * @author Brian Clozel
 */
class ClientHttpConnectorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ClientHttpConnectorAutoConfiguration.class));

	@Test
	void whenReactorIsAvailableThenReactorBeansAreDefined() {
		this.contextRunner.run((context) -> {
			BeanDefinition customizerDefinition = context.getBeanFactory()
					.getBeanDefinition("clientConnectorCustomizer");
			assertThat(customizerDefinition.isLazyInit()).isTrue();
			BeanDefinition connectorDefinition = context.getBeanFactory()
					.getBeanDefinition("reactorClientHttpConnector");
			assertThat(connectorDefinition.isLazyInit()).isTrue();
			assertThat(context).hasBean("reactorClientResourceFactory");
		});
	}

	@Test
	void whenReactorIsUnavailableThenJettyBeansAreDefined() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(HttpClient.class)).run((context) -> {
			BeanDefinition customizerDefinition = context.getBeanFactory()
					.getBeanDefinition("clientConnectorCustomizer");
			assertThat(customizerDefinition.isLazyInit()).isTrue();
			BeanDefinition connectorDefinition = context.getBeanFactory().getBeanDefinition("jettyClientHttpConnector");
			assertThat(connectorDefinition.isLazyInit()).isTrue();
			assertThat(context).hasBean("jettyClientResourceFactory");
		});
	}

	@Test
	void whenReactorAndJettyAreUnavailableThenHttpClientBeansAreDefined() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(HttpClient.class, ReactiveRequest.class))
				.run((context) -> {
					BeanDefinition customizerDefinition = context.getBeanFactory()
							.getBeanDefinition("clientConnectorCustomizer");
					assertThat(customizerDefinition.isLazyInit()).isTrue();
					BeanDefinition connectorDefinition = context.getBeanFactory()
							.getBeanDefinition("httpComponentsClientHttpConnector");
					assertThat(connectorDefinition.isLazyInit()).isTrue();
				});
	}

	@Test
	void shouldCreateHttpClientBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ReactorResourceFactory.class);
			assertThat(context).hasSingleBean(ReactorClientHttpConnector.class);
			WebClientCustomizer clientCustomizer = context.getBean(WebClientCustomizer.class);
			WebClient.Builder builder = mock(WebClient.Builder.class);
			clientCustomizer.customize(builder);
			verify(builder, times(1)).clientConnector(any(ReactorClientHttpConnector.class));
		});
	}

	@Test
	void shouldNotOverrideCustomClientConnector() {
		this.contextRunner.withUserConfiguration(CustomClientHttpConnectorConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(ClientHttpConnector.class).hasBean("customConnector")
					.doesNotHaveBean(ReactorResourceFactory.class);
			WebClientCustomizer clientCustomizer = context.getBean(WebClientCustomizer.class);
			WebClient.Builder builder = mock(WebClient.Builder.class);
			clientCustomizer.customize(builder);
			verify(builder, times(1)).clientConnector(any(ClientHttpConnector.class));
		});
	}

	@Test
	void shouldUseCustomReactorResourceFactory() {
		this.contextRunner.withUserConfiguration(CustomReactorResourceConfig.class)
				.run((context) -> assertThat(context).hasSingleBean(ReactorClientHttpConnector.class)
						.hasSingleBean(ReactorResourceFactory.class).hasBean("customReactorResourceFactory"));
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomClientHttpConnectorConfig {

		@Bean
		ClientHttpConnector customConnector() {
			return mock(ClientHttpConnector.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomReactorResourceConfig {

		@Bean
		ReactorResourceFactory customReactorResourceFactory() {
			return new ReactorResourceFactory();
		}

	}

}
