/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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
 * Tests for {@link WebClientAutoConfiguration}
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class WebClientAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void restTemplateWhenMessageConvertersDefinedShouldHaveMessageConverters() {
		load(HttpMessageConvertersAutoConfiguration.class, RestTemplateConfig.class);
		assertThat(this.context.getBeansOfType(RestTemplate.class)).hasSize(1);
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		List<HttpMessageConverter<?>> converters = this.context
				.getBean(HttpMessageConverters.class).getConverters();
		assertThat(restTemplate.getMessageConverters())
				.containsExactlyElementsOf(converters);
		assertThat(restTemplate.getRequestFactory())
				.isInstanceOf(HttpComponentsClientHttpRequestFactory.class);
	}

	@Test
	public void restTemplateWhenNoMessageConvertersDefinedShouldHaveDefaultMessageConverters() {
		load(RestTemplateConfig.class);
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		assertThat(restTemplate.getMessageConverters().size())
				.isEqualTo(new RestTemplate().getMessageConverters().size());
	}

	@Test
	public void restTemplateWhenHasCustomMessageConvertersShouldHaveMessageConverters() {
		load(CustomHttpMessageConverter.class,
				HttpMessageConvertersAutoConfiguration.class, RestTemplateConfig.class);
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		List<Class<?>> converterClasses = new ArrayList<Class<?>>();
		for (HttpMessageConverter<?> converter : restTemplate.getMessageConverters()) {
			converterClasses.add(converter.getClass());
		}
		assertThat(converterClasses).contains(CustomHttpMessageConverter.class);
	}

	@Test
	public void restTemplateWhenHasCustomBuilderShouldUseCustomBuilder() {
		load(RestTemplateConfig.class, CustomRestTemplateBuilderConfig.class);
		assertThat(this.context.getBeansOfType(RestTemplate.class)).hasSize(1);
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		assertThat(restTemplate.getMessageConverters()).hasSize(1);
		assertThat(restTemplate.getMessageConverters().get(0))
				.isInstanceOf(CustomHttpMessageConverter.class);
	}

	@Test
	public void restTemplateShouldApplyCustomizer() throws Exception {
		load(RestTemplateCustomizerConfig.class, RestTemplateConfig.class);
		RestTemplate restTemplate = this.context.getBean(RestTemplate.class);
		RestTemplateCustomizer customizer = this.context
				.getBean(RestTemplateCustomizer.class);
		verify(customizer).customize(restTemplate);
	}

	@Test
	public void builderShouldBeFreshForEachUse() throws Exception {
		load(DirtyRestTemplateConfig.class);
	}

	public void load(Class<?>... config) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(config);
		ctx.register(WebClientAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class RestTemplateConfig {

		@Bean
		public RestTemplate restTemplate(RestTemplateBuilder builder) {
			return builder.build();
		}

	}

	@Configuration
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
			builder.additionalCustomizers(new RestTemplateCustomizer() {

				@Override
				public void customize(RestTemplate restTemplate) {
					throw new IllegalStateException();
				}

			});
		}

	}

	@Configuration
	static class CustomRestTemplateBuilderConfig {

		@Bean
		public RestTemplateBuilder restTemplateBuilder() {
			return new RestTemplateBuilder()
					.messageConverters(new CustomHttpMessageConverter());
		}

	}

	@Configuration
	static class RestTemplateCustomizerConfig {

		@Bean
		public RestTemplateCustomizer restTemplateCustomizer() {
			return mock(RestTemplateCustomizer.class);
		}

	}

	static class CustomHttpMessageConverter extends StringHttpMessageConverter {

	}

}
