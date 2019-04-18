/*
 * Copyright 2012-2019 the original author or authors.
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

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link RestTemplateAutoConfiguration}
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class RestTemplateAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(RestTemplateAutoConfiguration.class));

	@Test
	public void restTemplateWhenMessageConvertersDefinedShouldHaveMessageConverters() {
		this.contextRunner
				.withConfiguration(AutoConfigurations
						.of(HttpMessageConvertersAutoConfiguration.class))
				.withUserConfiguration(RestTemplateConfig.class).run((context) -> {
					assertThat(context).hasSingleBean(RestTemplate.class);
					RestTemplate restTemplate = context.getBean(RestTemplate.class);
					List<HttpMessageConverter<?>> converters = context
							.getBean(HttpMessageConverters.class).getConverters();
					assertThat(restTemplate.getMessageConverters())
							.containsExactlyElementsOf(converters);
					assertThat(restTemplate.getRequestFactory())
							.isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
				});
	}

	@Test
	public void restTemplateWhenNoMessageConvertersDefinedShouldHaveDefaultMessageConverters() {
		this.contextRunner.withUserConfiguration(RestTemplateConfig.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(RestTemplate.class);
					RestTemplate restTemplate = context.getBean(RestTemplate.class);
					assertThat(restTemplate.getMessageConverters().size())
							.isEqualTo(new RestTemplate().getMessageConverters().size());
				});
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void restTemplateWhenHasCustomMessageConvertersShouldHaveMessageConverters() {
		this.contextRunner
				.withConfiguration(AutoConfigurations
						.of(HttpMessageConvertersAutoConfiguration.class))
				.withUserConfiguration(CustomHttpMessageConverter.class,
						RestTemplateConfig.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(RestTemplate.class);
					RestTemplate restTemplate = context.getBean(RestTemplate.class);
					assertThat(restTemplate.getMessageConverters())
							.extracting(HttpMessageConverter::getClass)
							.contains((Class) CustomHttpMessageConverter.class);
				});
	}

	@Test
	public void restTemplateWhenHasCustomBuilderShouldUseCustomBuilder() {
		this.contextRunner.withUserConfiguration(RestTemplateConfig.class,
				CustomRestTemplateBuilderConfig.class).run((context) -> {
					assertThat(context).hasSingleBean(RestTemplate.class);
					RestTemplate restTemplate = context.getBean(RestTemplate.class);
					assertThat(restTemplate.getMessageConverters()).hasSize(1);
					assertThat(restTemplate.getMessageConverters().get(0))
							.isInstanceOf(CustomHttpMessageConverter.class);
				});
	}

	@Test
	public void restTemplateShouldApplyCustomizer() {
		this.contextRunner.withUserConfiguration(RestTemplateConfig.class,
				RestTemplateCustomizerConfig.class).run((context) -> {
					assertThat(context).hasSingleBean(RestTemplate.class);
					RestTemplate restTemplate = context.getBean(RestTemplate.class);
					RestTemplateCustomizer customizer = context
							.getBean(RestTemplateCustomizer.class);
					verify(customizer).customize(restTemplate);
				});
	}

	@Test
	public void builderShouldBeFreshForEachUse() {
		this.contextRunner.withUserConfiguration(DirtyRestTemplateConfig.class)
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void whenServletWebApplicationRestTemplateBuilderIsConfigured() {
		new WebApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(RestTemplateAutoConfiguration.class))
				.run((context) -> assertThat(context)
						.hasSingleBean(RestTemplateBuilder.class));
	}

	@Test
	public void whenReactiveWebApplicationRestTemplateBuilderIsNotConfigured() {
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(RestTemplateAutoConfiguration.class))
				.run((context) -> assertThat(context)
						.doesNotHaveBean(RestTemplateBuilder.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class RestTemplateConfig {

		@Bean
		public RestTemplate restTemplate(RestTemplateBuilder builder) {
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DirtyRestTemplateConfig {

		@Bean
		public RestTemplate restTemplateOne(RestTemplateBuilder builder) {
			try {
				return builder.build();
			}
			finally {
				breakBuilderOnNextCall(builder);
			}
		}

		@Bean
		public RestTemplate restTemplateTwo(RestTemplateBuilder builder) {
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
		public RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder()
					.messageConverters(new CustomHttpMessageConverter());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RestTemplateCustomizerConfig {

		@Bean
		public RestTemplateCustomizer restTemplateCustomizer() {
			return mock(RestTemplateCustomizer.class);
		}

	}

	static class CustomHttpMessageConverter extends StringHttpMessageConverter {

	}

}
