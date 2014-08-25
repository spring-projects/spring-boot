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

import org.junit.After;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

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
 */
public class HttpMessageConvertersAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void customJacksonConverter() throws Exception {
		this.context.register(JacksonConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		MappingJackson2HttpMessageConverter converter = this.context
				.getBean(MappingJackson2HttpMessageConverter.class);
		assertEquals(this.context.getBean(ObjectMapper.class),
				converter.getObjectMapper());
		HttpMessageConverters converters = this.context
				.getBean(HttpMessageConverters.class);
		assertTrue(converters.getConverters().contains(converter));
	}

	@Configuration
	protected static class JacksonConfig {

		@Bean
		public MappingJackson2HttpMessageConverter jacksonMessaegConverter() {
			MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
			converter.setObjectMapper(objectMapper());
			return converter;
		}

		@Bean
		public ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

	}

	@Test
	public void customGsonConverter() throws Exception {
		this.context.register(GsonConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		GsonHttpMessageConverter converter = this.context
				.getBean(GsonHttpMessageConverter.class);
		assertEquals(this.context.getBean(Gson.class), converter.getGson());
		HttpMessageConverters converters = this.context
				.getBean(HttpMessageConverters.class);
		assertTrue(converters.getConverters().contains(converter));
	}

	@Configuration
	protected static class GsonConfig {

		@Bean
		public GsonHttpMessageConverter gsonMessageConverter() {
			GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
			converter.setGson(gson());
			return converter;
		}

		@Bean
		public Gson gson() {
			return new Gson();
		}
	}
}
