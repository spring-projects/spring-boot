/*
 * Copyright 2012-2014 the original author or authors.
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

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
		assertTrue(this.context.getBeansOfType(ObjectMapper.class).isEmpty());
		assertTrue(this.context.getBeansOfType(MappingJackson2HttpMessageConverter.class)
				.isEmpty());
		assertTrue(this.context.getBeansOfType(
				MappingJackson2XmlHttpMessageConverter.class).isEmpty());
	}

	@Test
	public void defaultJacksonConverter() throws Exception {
		this.context.register(JacksonObjectMapperConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();

		assertConverterBeanExists(MappingJackson2HttpMessageConverter.class,
				"mappingJackson2HttpMessageConverter");

		assertConverterBeanRegisteredWithHttpMessageConverters(MappingJackson2HttpMessageConverter.class);
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

		assertConverterBeanRegisteredWithHttpMessageConverters(MappingJackson2HttpMessageConverter.class);
		assertConverterBeanRegisteredWithHttpMessageConverters(MappingJackson2XmlHttpMessageConverter.class);
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
		assertTrue(this.context.getBeansOfType(Gson.class).isEmpty());
		assertTrue(this.context.getBeansOfType(GsonHttpMessageConverter.class).isEmpty());
	}

	@Test
	public void defaultGsonConverter() throws Exception {
		this.context.register(GsonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(GsonHttpMessageConverter.class,
				"gsonHttpMessageConverter");

		assertConverterBeanRegisteredWithHttpMessageConverters(GsonHttpMessageConverter.class);
	}

	@Test
	public void jacksonIsPreferredByDefaultWhenBothGsonAndJacksonAreAvailable() {
		this.context.register(GsonAutoConfiguration.class,
				JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(MappingJackson2HttpMessageConverter.class,
				"mappingJackson2HttpMessageConverter");
		assertConverterBeanRegisteredWithHttpMessageConverters(MappingJackson2HttpMessageConverter.class);
		assertEquals(0, this.context.getBeansOfType(GsonHttpMessageConverter.class)
				.size());
	}

	@Test
	public void gsonCanBePreferredWhenBothGsonAndJacksonAreAvailable() {
		this.context.register(GsonAutoConfiguration.class,
				JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.http.converters.preferred-json-mapper:gson");
		this.context.refresh();
		assertConverterBeanExists(GsonHttpMessageConverter.class,
				"gsonHttpMessageConverter");
		assertConverterBeanRegisteredWithHttpMessageConverters(GsonHttpMessageConverter.class);
		assertEquals(0,
				this.context.getBeansOfType(MappingJackson2HttpMessageConverter.class)
						.size());
	}

	@Test
	public void customGsonConverter() throws Exception {
		this.context.register(GsonAutoConfiguration.class, GsonConverterConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(GsonHttpMessageConverter.class,
				"customGsonMessageConverter");

		assertConverterBeanRegisteredWithHttpMessageConverters(GsonHttpMessageConverter.class);
	}

	@Test
	public void defaultStringConverter() throws Exception {
		this.context.register(HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(StringHttpMessageConverter.class,
				"stringHttpMessageConverter");
		assertConverterBeanRegisteredWithHttpMessageConverters(StringHttpMessageConverter.class);
	}

	@Test
	public void customStringConverter() throws Exception {
		this.context.register(StringConverterConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		assertConverterBeanExists(StringHttpMessageConverter.class,
				"customStringMessageConverter");

		assertConverterBeanRegisteredWithHttpMessageConverters(StringHttpMessageConverter.class);
	}

	private void assertConverterBeanExists(Class<?> type, String beanName) {
		assertEquals(1, this.context.getBeansOfType(type).size());
		List<String> beanNames = Arrays.asList(this.context.getBeanDefinitionNames());
		assertTrue(beanName + " not found in " + beanNames, beanNames.contains(beanName));
	}

	private void assertConverterBeanRegisteredWithHttpMessageConverters(Class<?> type) {
		Object converter = this.context.getBean(type);
		HttpMessageConverters converters = this.context
				.getBean(HttpMessageConverters.class);
		assertTrue(converters.getConverters().contains(converter));
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

}
