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

package org.springframework.boot.autoconfigure.web.client;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestClientAutoConfiguration}
 *
 * @author Arjen Poutsma
 * @author Moritz Halbritter
 */
class RestClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class));

	@Test
	void shouldSupplyBeans() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(HttpMessageConvertersRestClientCustomizer.class);
			assertThat(context).hasSingleBean(RestClientBuilderConfigurer.class);
			assertThat(context).hasSingleBean(RestClient.Builder.class);
		});
	}

	@Test
	void shouldSupplyRestClientSslIfSslBundlesIsThere() {
		this.contextRunner.withBean(SslBundles.class, () -> mock(SslBundles.class))
			.run((context) -> assertThat(context).hasSingleBean(RestClientSsl.class));
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
	void restClientWhenMessageConvertersDefinedShouldHaveMessageConverters() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(HttpMessageConvertersAutoConfiguration.class))
			.withUserConfiguration(RestClientConfig.class)
			.run((context) -> {
				RestClient restClient = context.getBean(RestClient.class);
				List<HttpMessageConverter<?>> expectedConverters = context.getBean(HttpMessageConverters.class)
					.getConverters();
				List<HttpMessageConverter<?>> actualConverters = (List<HttpMessageConverter<?>>) ReflectionTestUtils
					.getField(restClient, "messageConverters");
				assertThat(actualConverters).containsExactlyElementsOf(expectedConverters);
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

	@Configuration(proxyBeanMethods = false)
	static class CodecConfiguration {

		@Bean
		CodecCustomizer myCodecCustomizer() {
			return mock(CodecCustomizer.class);
		}

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

	static class CustomHttpMessageConverter extends StringHttpMessageConverter {

	}

}
