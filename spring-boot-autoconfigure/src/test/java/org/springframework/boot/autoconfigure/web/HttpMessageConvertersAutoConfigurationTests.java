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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.JacksonHttpMessageConvertersConfiguration.MappingJackson2HttpMessageConverterConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpMessageConvertersAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author David Liu
 * @author Andy Wilkinson
 * @author Sebastien Deleuze
 */
public class HttpMessageConvertersAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void noObjectMapperMeansNoConverter() throws Exception {
		this.context.register(HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(ObjectMapper.class)).isEmpty();
		assertThat(this.context.getBeansOfType(MappingJackson2HttpMessageConverter.class))
				.isEmpty();
		assertThat(
				this.context.getBeansOfType(MappingJackson2XmlHttpMessageConverter.class))
						.isEmpty();
	}

	@Test
	public void defaultJacksonConverter() throws Exception {
		this.context.register(JacksonObjectMapperConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();

		assertConverterBeanExists(MappingJackson2HttpMessageConverter.class,
				"mappingJackson2HttpMessageConverter");

		assertConverterBeanRegisteredWithHttpMessageConverters(
				MappingJackson2HttpMessageConverter.class);
	}

	@Test
	public void defaultJacksonConvertersWithBuilder() throws Exception {
		this.context.register(JacksonObjectMapperBuilderConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();

		assertConverterBeanExists(MappingJackson2HttpMessageConverter.class,
				"mappingJackson2HttpMessageConverter");
		assertConverterBeanExists(MappingJackson2XmlHttpMessageConverter.class,
				"mappingJackson2XmlHttpMessageConverter");

		assertConverterBeanRegisteredWithHttpMessageConverters(
				MappingJackson2HttpMessageConverter.class);
		assertConverterBeanRegisteredWithHttpMessageConverters(
				MappingJackson2XmlHttpMessageConverter.class);
	}

	@Test
	public void customJacksonConverter() throws Exception {
		this.context.register(JacksonObjectMapperConfig.class,
				JacksonConverterConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();

		assertConverterBeanExists(MappingJackson2HttpMessageConverter.class,
				"customJacksonMessageConverter");
	}

	@Test
	public void noGson() throws Exception {
		this.context.register(HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(Gson.class).isEmpty()).isTrue();
		assertThat(this.context.getBeansOfType(GsonHttpMessageConverter.class).isEmpty())
				.isTrue();
	}

	@Test
	public void defaultGsonConverter() throws Exception {
		this.context.register(GsonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(GsonHttpMessageConverter.class,
				"gsonHttpMessageConverter");

		assertConverterBeanRegisteredWithHttpMessageConverters(
				GsonHttpMessageConverter.class);
	}

	@Test
	public void jacksonIsPreferredByDefaultWhenBothGsonAndJacksonAreAvailable() {
		this.context.register(GsonAutoConfiguration.class, JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(MappingJackson2HttpMessageConverter.class,
				"mappingJackson2HttpMessageConverter");
		assertConverterBeanRegisteredWithHttpMessageConverters(
				MappingJackson2HttpMessageConverter.class);
		assertThat(this.context.getBeansOfType(GsonHttpMessageConverter.class)).isEmpty();
	}

	@Test
	public void gsonCanBePreferredWhenBothGsonAndJacksonAreAvailable() {
		this.context.register(GsonAutoConfiguration.class, JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.http.converters.preferred-json-mapper:gson");
		this.context.refresh();
		assertConverterBeanExists(GsonHttpMessageConverter.class,
				"gsonHttpMessageConverter");
		assertConverterBeanRegisteredWithHttpMessageConverters(
				GsonHttpMessageConverter.class);
		assertThat(this.context.getBeansOfType(MappingJackson2HttpMessageConverter.class))
				.isEmpty();
	}

	@Test
	public void customGsonConverter() throws Exception {
		this.context.register(GsonAutoConfiguration.class, GsonConverterConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(GsonHttpMessageConverter.class,
				"customGsonMessageConverter");

		assertConverterBeanRegisteredWithHttpMessageConverters(
				GsonHttpMessageConverter.class);
	}

	@Test
	public void defaultStringConverter() throws Exception {
		this.context.register(HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(StringHttpMessageConverter.class,
				"stringHttpMessageConverter");
		assertConverterBeanRegisteredWithHttpMessageConverters(
				StringHttpMessageConverter.class);
	}

	@Test
	public void customStringConverter() throws Exception {
		this.context.register(StringConverterConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(StringHttpMessageConverter.class,
				"customStringMessageConverter");

		assertConverterBeanRegisteredWithHttpMessageConverters(
				StringHttpMessageConverter.class);
	}

	@Test
	public void typeConstrainedConverterDoesNotPreventAutoConfigurationOfJacksonConverter()
			throws Exception {
		this.context.register(JacksonObjectMapperBuilderConfig.class,
				TypeConstrainedConverterConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();

		BeanDefinition beanDefinition = this.context
				.getBeanDefinition("mappingJackson2HttpMessageConverter");
		assertThat(beanDefinition.getFactoryBeanName()).isEqualTo(
				MappingJackson2HttpMessageConverterConfiguration.class.getName());
	}

	@Test
	public void typeConstrainedConverterFromSpringDataDoesNotPreventAutoConfigurationOfJacksonConverter()
			throws Exception {
		this.context.register(JacksonObjectMapperBuilderConfig.class,
				RepositoryRestMvcConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();

		Map<String, MappingJackson2HttpMessageConverter> beansOfType = this.context
				.getBeansOfType(MappingJackson2HttpMessageConverter.class);
		System.out.println(beansOfType);
		BeanDefinition beanDefinition = this.context
				.getBeanDefinition("mappingJackson2HttpMessageConverter");
		assertThat(beanDefinition.getFactoryBeanName()).isEqualTo(
				MappingJackson2HttpMessageConverterConfiguration.class.getName());
	}

	private void assertConverterBeanExists(Class<?> type, String beanName) {
		assertThat(this.context.getBeansOfType(type)).hasSize(1);
		List<String> beanNames = Arrays.asList(this.context.getBeanDefinitionNames());
		assertThat(beanNames).contains(beanName);
	}

	private void assertConverterBeanRegisteredWithHttpMessageConverters(Class<?> type) {
		Object converter = this.context.getBean(type);
		HttpMessageConverters converters = this.context
				.getBean(HttpMessageConverters.class);
		assertThat(converters.getConverters().contains(converter)).isTrue();
	}

	@Configuration
	protected static class JacksonObjectMapperConfig {
		@Bean
		public ObjectMapper objectMapper() {
			return new ObjectMapper();
		}
	}

	@Configuration
	protected static class JacksonObjectMapperBuilderConfig {

		@Bean
		public ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		public Jackson2ObjectMapperBuilder builder() {
			return new Jackson2ObjectMapperBuilder();
		}
	}

	@Configuration
	protected static class JacksonConverterConfig {

		@Bean
		public MappingJackson2HttpMessageConverter customJacksonMessageConverter(
				ObjectMapper objectMapper) {
			MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
			converter.setObjectMapper(objectMapper);
			return converter;
		}
	}

	@Configuration
	protected static class GsonConverterConfig {

		@Bean
		public GsonHttpMessageConverter customGsonMessageConverter(Gson gson) {
			GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
			converter.setGson(gson);
			return converter;
		}
	}

	@Configuration
	protected static class StringConverterConfig {

		@Bean
		public StringHttpMessageConverter customStringMessageConverter() {
			return new StringHttpMessageConverter();
		}
	}

	@Configuration
	protected static class TypeConstrainedConverterConfiguration {

		@Bean
		public TypeConstrainedMappingJackson2HttpMessageConverter typeConstrainedConverter() {
			return new TypeConstrainedMappingJackson2HttpMessageConverter(
					ResourceSupport.class);
		}
	}

}
