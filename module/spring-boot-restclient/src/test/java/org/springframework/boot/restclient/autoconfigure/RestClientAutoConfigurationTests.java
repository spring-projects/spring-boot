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

package org.springframework.boot.restclient.autoconfigure;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.reactive.ReactiveHttpClientAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestClientAutoConfiguration}
 *
 * @author Arjen Poutsma
 * @author Moritz Halbritter
 * @author Dmytro Nosan
 * @author Dmitry Sulman
 */
class RestClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class, HttpClientAutoConfiguration.class,
				ImperativeHttpClientAutoConfiguration.class, ReactiveHttpClientAutoConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RestClientBuilderConfigurer.class);
			assertThat(context).hasSingleBean(RestClient.Builder.class);
		});
	}

	@Test
	void shouldSupplyRestClientSslIfSslBundlesIsThereWithCustomHttpSettingsAndBuilder() {
		SslBundles sslBundles = mock(SslBundles.class);
		HttpClientSettings clientSettings = HttpClientSettings.defaults()
			.withRedirects(HttpRedirects.DONT_FOLLOW)
			.withConnectTimeout(Duration.ofHours(1))
			.withReadTimeout(Duration.ofDays(1))
			.withSslBundle(mock(SslBundle.class));
		ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder = mock(
				ClientHttpRequestFactoryBuilder.class);
		this.contextRunner.withBean(SslBundles.class, () -> sslBundles)
			.withBean(HttpClientSettings.class, () -> clientSettings)
			.withBean(ClientHttpRequestFactoryBuilder.class, () -> clientHttpRequestFactoryBuilder)
			.run((context) -> {
				assertThat(context).hasSingleBean(RestClientSsl.class);
				RestClientSsl restClientSsl = context.getBean(RestClientSsl.class);
				assertThat(restClientSsl).hasFieldOrPropertyWithValue("sslBundles", sslBundles);
				assertThat(restClientSsl).hasFieldOrPropertyWithValue("builder", clientHttpRequestFactoryBuilder);
				assertThat(restClientSsl).hasFieldOrPropertyWithValue("settings", clientSettings);
			});
	}

	@Test
	void shouldSupplyRestClientSslIfSslBundlesIsThereWithAutoConfiguredHttpSettingsAndBuilder() {
		SslBundles sslBundles = mock(SslBundles.class);
		this.contextRunner.withBean(SslBundles.class, () -> sslBundles).run((context) -> {
			assertThat(context).hasSingleBean(RestClientSsl.class)
				.hasSingleBean(HttpClientSettings.class)
				.hasSingleBean(ClientHttpRequestFactoryBuilder.class);
			RestClientSsl restClientSsl = context.getBean(RestClientSsl.class);
			assertThat(restClientSsl).hasFieldOrPropertyWithValue("sslBundles", sslBundles);
			assertThat(restClientSsl).hasFieldOrPropertyWithValue("builder",
					context.getBean(ClientHttpRequestFactoryBuilder.class));
			assertThat(restClientSsl).hasFieldOrPropertyWithValue("settings",
					context.getBean(HttpClientSettings.class));
		});
	}

	@Test
	void shouldCreateBuilder() {
		this.contextRunner.run((context) -> {
			RestClient.Builder builder = context.getBean(RestClient.Builder.class);
			RestClient restClient = builder.build();
			assertThat(restClient).isNotNull();
		});
	}

	@Test
	void configurerShouldCallCustomizers() {
		this.contextRunner.withUserConfiguration(RestClientCustomizerConfig.class).run((context) -> {
			RestClientBuilderConfigurer configurer = context.getBean(RestClientBuilderConfigurer.class);
			RestClientCustomizer customizer = context.getBean("restClientCustomizer", RestClientCustomizer.class);
			Builder builder = RestClient.builder();
			configurer.configure(builder);
			then(customizer).should().customize(builder);
		});
	}

	@Test
	void restClientShouldApplyCustomizers() {
		this.contextRunner.withUserConfiguration(RestClientCustomizerConfig.class).run((context) -> {
			RestClient.Builder builder = context.getBean(RestClient.Builder.class);
			RestClientCustomizer customizer = context.getBean("restClientCustomizer", RestClientCustomizer.class);
			builder.build();
			then(customizer).should().customize(any(RestClient.Builder.class));
		});
	}

	@Test
	void shouldGetPrototypeScopedBean() {
		this.contextRunner.withUserConfiguration(RestClientCustomizerConfig.class).run((context) -> {
			RestClient.Builder firstBuilder = context.getBean(RestClient.Builder.class);
			RestClient.Builder secondBuilder = context.getBean(RestClient.Builder.class);
			assertThat(firstBuilder).isNotEqualTo(secondBuilder);
		});
	}

	@Test
	void shouldNotCreateClientBuilderIfAlreadyPresent() {
		this.contextRunner.withUserConfiguration(CustomRestClientBuilderConfig.class).run((context) -> {
			RestClient.Builder builder = context.getBean(RestClient.Builder.class);
			assertThat(builder).isInstanceOf(MyRestClientBuilder.class);
		});
	}

	@Test
	@SuppressWarnings("unchecked")
	void restClientWhenNoMessageConvertersDefinedShouldHaveDefaultMessageConverters() {
		this.contextRunner.withUserConfiguration(RestClientConfig.class).run((context) -> {
			RestClient restClient = context.getBean(RestClient.class);
			RestClient defaultRestClient = RestClient.builder().build();
			List<HttpMessageConverter<?>> actualConverters = (List<HttpMessageConverter<?>>) ReflectionTestUtils
				.getField(restClient, "messageConverters");
			List<HttpMessageConverter<?>> expectedConverters = (List<HttpMessageConverter<?>>) ReflectionTestUtils
				.getField(defaultRestClient, "messageConverters");
			assertThat(actualConverters).hasSameSizeAs(expectedConverters);
		});
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void restClientWhenHasCustomMessageConvertersShouldHaveMessageConverters() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.withUserConfiguration(CustomHttpMessageConverter.class, RestClientConfig.class)
			.run((context) -> {
				RestClient restClient = context.getBean(RestClient.class);
				List<HttpMessageConverter<?>> actualConverters = (List<HttpMessageConverter<?>>) ReflectionTestUtils
					.getField(restClient, "messageConverters");
				assertThat(actualConverters).extracting(HttpMessageConverter::getClass)
					.contains((Class) CustomHttpMessageConverter.class);
			});
	}

	@Test
	void whenHasFactoryProperty() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.withUserConfiguration(RestClientConfig.class)
			.withPropertyValues("spring.http.clients.imperative.factory=simple")
			.run((context) -> {
				assertThat(context).hasSingleBean(RestClient.class);
				RestClient restClient = context.getBean(RestClient.class);
				assertThat(restClient).extracting("clientRequestFactory")
					.isInstanceOf(SimpleClientHttpRequestFactory.class);
			});
	}

	@Test
	void shouldSupplyRestClientBuilderConfigurerWithCustomSettings() {
		HttpClientSettings clientSettings = HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW);
		ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder = mock(
				ClientHttpRequestFactoryBuilder.class);
		RestClientCustomizer customizer1 = mock(RestClientCustomizer.class);
		RestClientCustomizer customizer2 = mock(RestClientCustomizer.class);
		HttpMessageConvertersRestClientCustomizer httpMessageConverterCustomizer = mock(
				HttpMessageConvertersRestClientCustomizer.class);
		this.contextRunner.withBean(HttpClientSettings.class, () -> clientSettings)
			.withBean(ClientHttpRequestFactoryBuilder.class, () -> clientHttpRequestFactoryBuilder)
			.withBean("customizer1", RestClientCustomizer.class, () -> customizer1)
			.withBean("customizer2", RestClientCustomizer.class, () -> customizer2)
			.withBean("httpMessageConverterCustomizer", HttpMessageConvertersRestClientCustomizer.class,
					() -> httpMessageConverterCustomizer)
			.run((context) -> {
				assertThat(context).hasSingleBean(RestClientBuilderConfigurer.class)
					.hasSingleBean(HttpClientSettings.class)
					.hasSingleBean(ClientHttpRequestFactoryBuilder.class);
				RestClientBuilderConfigurer configurer = context.getBean(RestClientBuilderConfigurer.class);
				assertThat(configurer).hasFieldOrPropertyWithValue("requestFactoryBuilder",
						clientHttpRequestFactoryBuilder);
				assertThat(configurer).hasFieldOrPropertyWithValue("clientSettings", clientSettings);
				assertThat(configurer).hasFieldOrPropertyWithValue("customizers",
						List.of(customizer1, customizer2, httpMessageConverterCustomizer));
			});
	}

	@Test
	void shouldSupplyRestClientBuilderConfigurerWithAutoConfiguredHttpSettings() {
		RestClientCustomizer customizer1 = mock(RestClientCustomizer.class);
		RestClientCustomizer customizer2 = mock(RestClientCustomizer.class);
		this.contextRunner.withBean("customizer1", RestClientCustomizer.class, () -> customizer1)
			.withBean("customizer2", RestClientCustomizer.class, () -> customizer2)
			.run((context) -> {
				assertThat(context).hasSingleBean(RestClientBuilderConfigurer.class)
					.hasSingleBean(HttpClientSettings.class)
					.hasSingleBean(ClientHttpRequestFactoryBuilder.class);
				RestClientBuilderConfigurer configurer = context.getBean(RestClientBuilderConfigurer.class);
				assertThat(configurer).hasFieldOrPropertyWithValue("requestFactoryBuilder",
						context.getBean(ClientHttpRequestFactoryBuilder.class));
				assertThat(configurer).hasFieldOrPropertyWithValue("clientSettings",
						context.getBean(HttpClientSettings.class));
				assertThat(configurer).hasFieldOrPropertyWithValue("customizers", List.of(customizer1, customizer2));
			});
	}

	@Test
	void whenServletWebApplicationRestClientIsConfigured() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(RestClientBuilderConfigurer.class);
				assertThat(context).hasSingleBean(RestClient.Builder.class);
			});
	}

	@Test
	void clientHttpMessageConverterCustomizersAreAppliedInOrder() {
		this.contextRunner.withUserConfiguration(ClientHttpMessageConverterCustomizersConfiguration.class)
			.run((context) -> {
				context.getBean(RestClient.Builder.class).build();
				ClientHttpMessageConvertersCustomizer customizer1 = context.getBean("customizer1",
						ClientHttpMessageConvertersCustomizer.class);
				ClientHttpMessageConvertersCustomizer customizer2 = context.getBean("customizer2",
						ClientHttpMessageConvertersCustomizer.class);
				ClientHttpMessageConvertersCustomizer customizer3 = context.getBean("customizer3",
						ClientHttpMessageConvertersCustomizer.class);
				InOrder inOrder = inOrder(customizer1, customizer2, customizer3);
				inOrder.verify(customizer3).customize(any(ClientBuilder.class));
				inOrder.verify(customizer1).customize(any(ClientBuilder.class));
				inOrder.verify(customizer2).customize(any(ClientBuilder.class));
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class RestClientCustomizerConfig {

		@Bean
		RestClientCustomizer restClientCustomizer() {
			return mock(RestClientCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestClientBuilderConfig {

		@Bean
		MyRestClientBuilder myRestClientBuilder() {
			return mock(MyRestClientBuilder.class);
		}

	}

	interface MyRestClientBuilder extends RestClient.Builder {

	}

	@Configuration(proxyBeanMethods = false)
	static class RestClientConfig {

		@Bean
		RestClient restClient(RestClient.Builder restClientBuilder) {
			return restClientBuilder.build();
		}

	}

	static class CustomHttpMessageConverter extends ByteArrayHttpMessageConverter {

	}

	@Configuration(proxyBeanMethods = false)
	static class ClientHttpMessageConverterCustomizersConfiguration {

		@Bean
		@Order(-5)
		ClientHttpMessageConvertersCustomizer customizer1() {
			return mock(ClientHttpMessageConvertersCustomizer.class);
		}

		@Bean
		@Order(5)
		ClientHttpMessageConvertersCustomizer customizer2() {
			return mock(ClientHttpMessageConvertersCustomizer.class);
		}

		@Bean
		@Order(-10)
		ClientHttpMessageConvertersCustomizer customizer3() {
			return mock(ClientHttpMessageConvertersCustomizer.class);
		}

	}

}
