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

package org.springframework.boot.restclient.autoconfigure.service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.util.HashMap;
import java.util.Map;

import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;

import org.springframework.aop.Advisor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;
import org.springframework.web.service.registry.ImportHttpServices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link HttpServiceClientAutoConfiguration},
 * {@link RestClientPropertiesHttpServiceGroupConfigurer} and
 * {@link RestClientCustomizerHttpServiceGroupConfigurer}.
 *
 * @author Phillip Webb
 */
class HttpServiceClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(HttpServiceClientAutoConfiguration.class,
				HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class));

	@Test
	void configuresClientFromProperties() {
		this.contextRunner
			.withPropertyValues("spring.http.client.service.base-url=https://example.com",
					"spring.http.client.service.default-header.test=true",
					"spring.http.client.service.group.one.base-url=https://example.com/one",
					"spring.http.client.service.group.two.default-header.two=iam2")
			.withUserConfiguration(HttpClientConfiguration.class, MockRestServiceServerConfiguration.class)
			.run((context) -> {
				HttpServiceProxyRegistry serviceProxyRegistry = context.getBean(HttpServiceProxyRegistry.class);
				assertThat(serviceProxyRegistry.getGroupNames()).containsOnly("one", "two");
				MockRestServiceServerConfiguration mockServers = context
					.getBean(MockRestServiceServerConfiguration.class);
				MockRestServiceServer serverOne = mockServers.getMock("one");
				serverOne.expect(requestTo("https://example.com/one/hello"))
					.andExpect(header("test", "true"))
					.andRespond(withSuccess().body("world!"));
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				assertThat(clientOne.hello()).isEqualTo("world!");
				MockRestServiceServer serverTwo = mockServers.getMock("two");
				serverTwo.expect((request) -> request.getURI().toString().equals("https://example.com/"))
					.andExpect(header("test", "true"))
					.andExpect(header("two", "iam2"))
					.andRespond(withSuccess().body("boot!"));
				TestClientTwo clientTwo = context.getBean(TestClientTwo.class);
				assertThat(clientTwo.there()).isEqualTo("boot!");
			});
	}

	@Test
	void whenHasUserDefinedRequestFactoryBuilder() {
		this.contextRunner.withPropertyValues("spring.http.client.service.base-url=https://example.com")
			.withUserConfiguration(HttpClientConfiguration.class, RequestFactoryBuilderConfiguration.class)
			.run((context) -> {
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				assertThat(getJdkHttpClient(clientOne).followRedirects()).isEqualTo(Redirect.NEVER);
			});
	}

	@Test
	void whenHasUserDefinedRequestFactorySettings() {
		this.contextRunner
			.withPropertyValues("spring.http.client.service.base-url=https://example.com",
					"spring.http.client.factory=jdk")
			.withUserConfiguration(HttpClientConfiguration.class, RequestFactorySettingsConfiguration.class)
			.run((context) -> {
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				assertThat(getJdkHttpClient(clientOne).followRedirects()).isEqualTo(Redirect.NEVER);
			});
	}

	@Test
	void whenHasUserDefinedRestClientCustomizer() {
		this.contextRunner.withPropertyValues("spring.http.client.service.base-url=https://example.com")
			.withUserConfiguration(HttpClientConfiguration.class, MockRestServiceServerConfiguration.class,
					RestClientCustomizerConfiguration.class)
			.run((context) -> {
				MockRestServiceServerConfiguration mockServers = context
					.getBean(MockRestServiceServerConfiguration.class);
				MockRestServiceServer serverOne = mockServers.getMock("one");
				serverOne.expect(requestTo("https://example.com/hello"))
					.andExpect(header("customized", "true"))
					.andRespond(withSuccess().body("world!"));
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				assertThat(clientOne.hello()).isEqualTo("world!");
			});
	}

	@Test
	void whenHasUserDefinedHttpServiceGroupConfigurer() {
		this.contextRunner.withPropertyValues("spring.http.client.service.base-url=https://example.com")
			.withUserConfiguration(HttpClientConfiguration.class, MockRestServiceServerConfiguration.class,
					HttpServiceGroupConfigurerConfiguration.class)
			.run((context) -> {
				MockRestServiceServerConfiguration mockServers = context
					.getBean(MockRestServiceServerConfiguration.class);
				MockRestServiceServer serverOne = mockServers.getMock("one");
				serverOne.expect(requestTo("https://example.com/hello"))
					.andExpect(header("customizedgroup", "true"))
					.andRespond(withSuccess().body("world!"));
				TestClientOne clientOne = context.getBean(TestClientOne.class);
				assertThat(clientOne.hello()).isEqualTo("world!");
			});
	}

	@Test
	void whenHasNoHttpServiceProxyRegistryBean() {
		this.contextRunner.withPropertyValues("spring.http.client.service.base-url=https://example.com")
			.run((context) -> assertThat(context).doesNotHaveBean(HttpServiceProxyRegistry.class));
	}

	private HttpClient getJdkHttpClient(Object proxy) {
		return (HttpClient) Extractors.byName("clientRequestFactory.httpClient").apply(getRestClient(proxy));
	}

	private RestClient getRestClient(Object proxy) {
		InvocationHandler handler = Proxy.getInvocationHandler(proxy);
		Advisor[] advisors = (Advisor[]) Extractors.byName("advised.advisors").apply(handler);
		Map<?, ?> serviceMethods = (Map<?, ?>) Extractors.byName("advice.httpServiceMethods").apply(advisors[0]);
		Object serviceMethod = serviceMethods.values().iterator().next();
		return (RestClient) Extractors.byName("responseFunction.responseFunction.arg$1.restClient")
			.apply(serviceMethod);
	}

	@Configuration(proxyBeanMethods = false)
	static class MockRestServiceServerConfiguration {

		private Map<String, MockRestServiceServer> mocks = new HashMap<>();

		@Bean
		RestClientHttpServiceGroupConfigurer mockServerConfigurer() {
			return (groups) -> groups.forEachClient(this::addMock);
		}

		private MockRestServiceServer addMock(HttpServiceGroup group, Builder client) {
			return this.mocks.put(group.name(), MockRestServiceServer.bindTo(client).build());
		}

		MockRestServiceServer getMock(String name) {
			return this.mocks.get(name);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(group = "one", types = TestClientOne.class)
	@ImportHttpServices(group = "two", types = TestClientTwo.class)
	static class HttpClientConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class RequestFactoryBuilderConfiguration {

		@Bean
		ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder() {
			return ClientHttpRequestFactoryBuilder.jdk()
				.withHttpClientCustomizer((httpClient) -> httpClient.followRedirects(Redirect.NEVER));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RequestFactorySettingsConfiguration {

		@Bean
		ClientHttpRequestFactorySettings requestFactorySettings() {
			return ClientHttpRequestFactorySettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RestClientCustomizerConfiguration {

		@Bean
		RestClientCustomizer restClientCustomizer() {
			return (builder) -> builder.defaultHeader("customized", "true");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpServiceGroupConfigurerConfiguration {

		@Bean
		RestClientHttpServiceGroupConfigurer restClientHttpServiceGroupConfigurer() {
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
