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

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.beans.factory.support.BeanDefinitionOverrideException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.restclient.RestTemplateCustomizer;
import org.springframework.boot.restclient.RestTemplateRequestCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters.ClientBuilder;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestTemplateAutoConfiguration}
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class RestTemplateAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class, HttpClientAutoConfiguration.class,
				ImperativeHttpClientAutoConfiguration.class));

	@Test
	void restTemplateBuilderConfigurerShouldBeLazilyDefined() {
		this.contextRunner.run((context) -> assertThat(
				context.getBeanFactory().getBeanDefinition("restTemplateBuilderConfigurer").isLazyInit())
			.isTrue());
	}

	@Test
	void shouldFailOnCustomRestTemplateBuilderConfigurer() {
		this.contextRunner.withUserConfiguration(RestTemplateBuilderConfigurerConfig.class)
			.run((context) -> assertThat(context).getFailure()
				.isInstanceOf(BeanDefinitionOverrideException.class)
				.hasMessageContaining("with name 'restTemplateBuilderConfigurer'"));
	}

	@Test
	void restTemplateBuilderShouldBeLazilyDefined() {
		this.contextRunner
			.run((context) -> assertThat(context.getBeanFactory().getBeanDefinition("restTemplateBuilder").isLazyInit())
				.isTrue());
	}

	@Test
	void restTemplateWhenNoMessageConvertersDefinedShouldHaveDefaultMessageConverters() {
		this.contextRunner.withUserConfiguration(RestTemplateConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(RestTemplate.class);
			RestTemplate restTemplate = context.getBean(RestTemplate.class);
			assertThat(restTemplate.getMessageConverters()).hasSameSizeAs(new RestTemplate().getMessageConverters());
		});
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void restTemplateWhenHasCustomMessageConvertersShouldHaveMessageConverters() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.withUserConfiguration(CustomHttpMessageConverter.class, RestTemplateConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(RestTemplate.class);
				RestTemplate restTemplate = context.getBean(RestTemplate.class);
				assertThat(restTemplate.getMessageConverters()).extracting(HttpMessageConverter::getClass)
					.contains((Class) CustomHttpMessageConverter.class);
			});
	}

	@Test
	void restTemplateShouldApplyCustomizer() {
		this.contextRunner.withUserConfiguration(RestTemplateConfig.class, RestTemplateCustomizerConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(RestTemplate.class);
				RestTemplate restTemplate = context.getBean(RestTemplate.class);
				RestTemplateCustomizer customizer = context.getBean(RestTemplateCustomizer.class);
				then(customizer).should().customize(restTemplate);
			});
	}

	@Test
	void restTemplateWhenHasCustomBuilderShouldUseCustomBuilder() {
		this.contextRunner
			.withUserConfiguration(RestTemplateConfig.class, CustomRestTemplateBuilderConfig.class,
					RestTemplateCustomizerConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(RestTemplate.class);
				RestTemplate restTemplate = context.getBean(RestTemplate.class);
				assertThat(restTemplate.getMessageConverters()).hasSize(1);
				assertThat(restTemplate.getMessageConverters().get(0)).isInstanceOf(CustomHttpMessageConverter.class);
				then(context.getBean(RestTemplateCustomizer.class)).shouldHaveNoInteractions();
			});
	}

	@Test
	void restTemplateWhenHasCustomBuilderCouldReuseBuilderConfigurer() {
		this.contextRunner
			.withUserConfiguration(RestTemplateConfig.class, CustomRestTemplateBuilderWithConfigurerConfig.class,
					RestTemplateCustomizerConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(RestTemplate.class);
				RestTemplate restTemplate = context.getBean(RestTemplate.class);
				assertThat(restTemplate.getMessageConverters()).hasSize(1);
				assertThat(restTemplate.getMessageConverters().get(0)).isInstanceOf(CustomHttpMessageConverter.class);
				RestTemplateCustomizer customizer = context.getBean(RestTemplateCustomizer.class);
				then(customizer).should().customize(restTemplate);
			});
	}

	@Test
	void restTemplateShouldApplyRequestCustomizer() {
		this.contextRunner.withUserConfiguration(RestTemplateRequestCustomizerConfig.class).run((context) -> {
			RestTemplateBuilder builder = context.getBean(RestTemplateBuilder.class);
			ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);
			MockClientHttpRequest request = new MockClientHttpRequest();
			request.setResponse(new MockClientHttpResponse(new byte[0], HttpStatus.OK));
			given(requestFactory.createRequest(any(), any())).willReturn(request);
			RestTemplate restTemplate = builder.requestFactory(() -> requestFactory).build();
			restTemplate.getForEntity("http://localhost:8080/test", String.class);
			assertThat(request.getHeaders().headerSet()).contains(entry("spring", Collections.singletonList("boot")));
		});
	}

	@Test
	void builderShouldBeFreshForEachUse() {
		this.contextRunner.withUserConfiguration(DirtyRestTemplateConfig.class)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void whenServletWebApplicationRestTemplateBuilderIsConfigured() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(RestTemplateBuilder.class)
				.hasSingleBean(RestTemplateBuilderConfigurer.class));
	}

	@Test
	void whenReactiveWebApplicationRestTemplateBuilderIsNotConfigured() {
		new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(RestTemplateBuilder.class)
				.doesNotHaveBean(RestTemplateBuilderConfigurer.class));
	}

	@Test
	void whenHasFactoryProperty() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.withUserConfiguration(RestTemplateConfig.class)
			.withPropertyValues("spring.http.clients.imperative.factory=simple")
			.run((context) -> {
				assertThat(context).hasSingleBean(RestTemplate.class);
				RestTemplate restTemplate = context.getBean(RestTemplate.class);
				assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
			});
	}

	@Test
	void clientHttpMessageConverterCustomizersAreAppliedInOrder() {
		this.contextRunner.withUserConfiguration(ClientHttpMessageConverterCustomizersConfiguration.class)
			.run((context) -> {
				context.getBean(RestTemplateBuilder.class).build();
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
	static class RestTemplateConfig {

		@Bean
		RestTemplate restTemplate(RestTemplateBuilder builder) {
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DirtyRestTemplateConfig {

		@Bean
		RestTemplate restTemplateOne(RestTemplateBuilder builder) {
			try {
				return builder.build();
			}
			finally {
				breakBuilderOnNextCall(builder);
			}
		}

		@Bean
		RestTemplate restTemplateTwo(RestTemplateBuilder builder) {
			try {
				return builder.build();
			}
			finally {
				breakBuilderOnNextCall(builder);
			}
		}

		private void breakBuilderOnNextCall(RestTemplateBuilder builder) {
			builder.additionalCustomizers((restTemplate) -> {
				throw new IllegalStateException();
			});
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestTemplateBuilderConfig {

		@Bean
		RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder().messageConverters(new CustomHttpMessageConverter());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestTemplateBuilderWithConfigurerConfig {

		@Bean
		RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer) {
			return configurer.configure(new RestTemplateBuilder()).messageConverters(new CustomHttpMessageConverter());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RestTemplateCustomizerConfig {

		@Bean
		RestTemplateCustomizer restTemplateCustomizer() {
			return mock(RestTemplateCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RestTemplateRequestCustomizerConfig {

		@Bean
		RestTemplateRequestCustomizer<?> restTemplateRequestCustomizer() {
			return (request) -> request.getHeaders().add("spring", "boot");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RestTemplateBuilderConfigurerConfig {

		@Bean
		RestTemplateBuilderConfigurer restTemplateBuilderConfigurer() {
			return new RestTemplateBuilderConfigurer();
		}

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

	static class CustomHttpMessageConverter extends ByteArrayHttpMessageConverter {

	}

}
