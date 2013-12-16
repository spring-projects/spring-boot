/*
 * Copyright 2012-2013 the original author or authors.
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

import java.io.IOException;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * @author Dave Syer
 */
public class HttpMessageConvertersAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void customJacksonConverter() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(JacksonConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		MappingJackson2HttpMessageConverter converter = this.context
				.getBean(MappingJackson2HttpMessageConverter.class);
		assertEquals(this.context.getBean(ObjectMapper.class),
				converter.getObjectMapper());
		HttpMessageConverters converters = this.context
				.getBean(HttpMessageConverters.class);
		assertTrue(converters.getMessageConverters().contains(converter));
	}

	@Test
	public void customJacksonModules() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(ModulesConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);

		@SuppressWarnings({ "unchecked", "unused" })
		ObjectMapper result = verify(mapper).registerModules(
				(Iterable<Module>) argThat(hasItem(this.context.getBean(Module.class))));
	}

	@Test
	public void doubleModuleRegistration() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(DoubleModulesConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertEquals("{\"foo\":\"bar\"}", mapper.writeValueAsString(new Foo()));
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

	@Configuration
	protected static class ModulesConfig {

		@Bean
		public Module jacksonModule() {
			return new SimpleModule();
		}

		@Bean
		@Primary
		public ObjectMapper objectMapper() {
			return Mockito.mock(ObjectMapper.class);
		}

	}

	@Configuration
	protected static class DoubleModulesConfig {

		@Bean
		public Module jacksonModule() {
			SimpleModule module = new SimpleModule();
			module.addSerializer(Foo.class, new JsonSerializer<Foo>() {

				@Override
				public void serialize(Foo value, JsonGenerator jgen,
						SerializerProvider provider) throws IOException,
						JsonProcessingException {
					jgen.writeStartObject();
					jgen.writeStringField("foo", "bar");
					jgen.writeEndObject();
				}
			});
			return module;
		}

		@Bean
		@Primary
		public ObjectMapper objectMapper() {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(jacksonModule());
			return mapper;
		}

	}

	protected static class Foo {

		private String name;

		private Foo() {

		}

		static Foo create() {
			return new Foo();
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
