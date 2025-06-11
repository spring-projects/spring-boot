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

package org.springframework.boot.webclient.autoconfigure.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.util.List;
import java.util.Map;

import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.reactive.ClientHttpConnectorAutoConfiguration;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.boot.webclient.autoconfigure.WebClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;
import org.springframework.web.service.registry.ImportHttpServices;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHttpServiceClientAutoConfiguration},
 * {@link WebClientPropertiesHttpServiceGroupConfigurer} and
 * {@link WebClientCustomizerHttpServiceGroupConfigurer}.
 *
 * @author Phillip Webb
 */
class ReactiveHttpServiceClientAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ReactiveHttpServiceClientAutoConfiguration.class,
				ClientHttpConnectorAutoConfiguration.class, WebClientAutoConfiguration.class));

	@Test
	void configuresClientFromProperties() {
		this.contextRunner
			.withPropertyValues("spring.http.reactiveclient.service.base-url=https://example.com",
					"spring.http.reactiveclient.service.default-header.test=true",
					"spring.http.reactiveclient.service.group.one.base-url=https://example.com/one",
					"spring.http.reactiveclient.service.group.two.default-header.two=iam2")
			.withUserConfiguration(HttpClientConfiguration.class)
			.run((context) -> {
				HttpServiceProxyRegistry serviceProxyRegistry = context.getBean(HttpServiceProxyRegistry.class);
				assertThat(serviceProxyRegistry.getGroupNames()).containsOnly("one", "two");
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				WebClient webClientOne = getWebClient(clientOne);
				assertThat(getUriComponentsBuilder(webClientOne).toUriString()).isEqualTo("https://example.com/one");
				assertThat(getHttpHeaders(webClientOne).headerSet())
					.containsExactlyInAnyOrder(Map.entry("test", List.of("true")));
				TestClientTwo clientTwo = context.getBean(TestClientTwo.class);
				WebClient webClientTwo = getWebClient(clientTwo);
				assertThat(getUriComponentsBuilder(webClientTwo).toUriString()).isEqualTo("https://example.com");
				assertThat(getHttpHeaders(webClientTwo).headerSet())
					.containsExactlyInAnyOrder(Map.entry("test", List.of("true")), Map.entry("two", List.of("iam2")));
			});
	}

	@Test
	void whenHasUserDefinedHttpConnectorBuilder() {
		this.contextRunner.withPropertyValues("spring.http.reactiveclient.service.base-url=https://example.com")
			.withUserConfiguration(HttpClientConfiguration.class, HttpConnectorBuilderConfiguration.class)
			.run((context) -> {
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				assertThat(getJdkHttpClient(clientOne).followRedirects()).isEqualTo(Redirect.NEVER);
			});
	}

	@Test
	void whenHasUserDefinedRequestFactorySettings() {
		this.contextRunner
			.withPropertyValues("spring.http.reactiveclient.service.base-url=https://example.com",
					"spring.http.reactiveclient.connector=jdk")
			.withUserConfiguration(HttpClientConfiguration.class, HttpConnectorSettingsConfiguration.class)
			.run((context) -> {
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				assertThat(getJdkHttpClient(clientOne).followRedirects()).isEqualTo(Redirect.NEVER);
			});
	}

	@Test
	void whenHasUserDefinedWebClientCustomizer() {
		this.contextRunner.withPropertyValues("spring.http.reactiveclient.service.base-url=https://example.com")
			.withUserConfiguration(HttpClientConfiguration.class, WebClientCustomizerConfiguration.class)
			.run((context) -> {
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				WebClient webClientOne = getWebClient(clientOne);
				assertThat(getHttpHeaders(webClientOne).headerSet())
					.containsExactlyInAnyOrder(Map.entry("customized", List.of("true")));
			});
	}

	@Test
	void whenHasUserDefinedHttpServiceGroupConfigurer() {
		this.contextRunner.withPropertyValues("spring.http.reactiveclient.service.base-url=https://example.com")
			.withUserConfiguration(HttpClientConfiguration.class, HttpServiceGroupConfigurerConfiguration.class)
			.run((context) -> {
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				WebClient webClientOne = getWebClient(clientOne);
				assertThat(getHttpHeaders(webClientOne).headerSet())
					.containsExactlyInAnyOrder(Map.entry("customizedgroup", List.of("true")));
			});
	}

	@Test
	void whenHasNoHttpServiceProxyRegistryBean() {
		this.contextRunner.withPropertyValues("spring.http.client.reactiveclient.base-url=https://example.com")
			.run((context) -> assertThat(context).doesNotHaveBean(HttpServiceProxyRegistry.class));
	}

	private HttpClient getJdkHttpClient(Object proxy) {
		return (HttpClient) Extractors.byName("builder.connector.httpClient").apply(getWebClient(proxy));
	}

	private HttpHeaders getHttpHeaders(WebClient webClient) {
		return (HttpHeaders) Extractors.byName("defaultHeaders").apply(webClient);
	}

	private UriComponentsBuilder getUriComponentsBuilder(WebClient webClient) {
		return (UriComponentsBuilder) Extractors.byName("uriBuilderFactory.baseUri").apply(webClient);
	}

	private WebClient getWebClient(Object proxy) {
		InvocationHandler handler = Proxy.getInvocationHandler(proxy);
		Advisor[] advisors = (Advisor[]) Extractors.byName("advised.advisors").apply(handler);
		Map<?, ?> serviceMethods = (Map<?, ?>) Extractors.byName("advice.httpServiceMethods").apply(advisors[0]);
		Object serviceMethod = serviceMethods.values().iterator().next();
		return (WebClient) Extractors.byName("responseFunction.responseFunction.arg$1.webClient").apply(serviceMethod);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "one", types = TestClientOne.class, clientType = ClientType.WEB_CLIENT)
	@ImportHttpServices(group = "two", types = TestClientTwo.class, clientType = ClientType.WEB_CLIENT)
	static class HttpClientConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpConnectorBuilderConfiguration {

		@Bean
		ClientHttpConnectorBuilder<?> httpConnectorBuilder() {
			return ClientHttpConnectorBuilder.jdk()
				.withHttpClientCustomizer((httpClient) -> httpClient.followRedirects(Redirect.NEVER));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpConnectorSettingsConfiguration {

		@Bean
		ClientHttpConnectorSettings httpConnectorSettings() {
			return ClientHttpConnectorSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class WebClientCustomizerConfiguration {

		@Bean
		WebClientCustomizer webClientCustomizer() {
			return (builder) -> builder.defaultHeader("customized", "true");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpServiceGroupConfigurerConfiguration {

		@Bean
		WebClientHttpServiceGroupConfigurer restClientHttpServiceGroupConfigurer() {
			return (groups) -> groups.filterByName("one")
				.forEachClient((group, builder) -> builder.defaultHeader("customizedgroup", "true"));
		}

	}

	interface TestClientOne {

		@GetExchange("/hello")
		String hello();

	}

	interface TestClientTwo {

		@GetExchange("/there")
		String there();

	}

}
