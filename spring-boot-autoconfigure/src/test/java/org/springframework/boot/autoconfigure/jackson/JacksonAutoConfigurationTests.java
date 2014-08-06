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

package org.springframework.boot.autoconfigure.jackson;

import java.io.IOException;
import java.util.Map;

import org.joda.time.LocalDateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JacksonAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 */
public class JacksonAutoConfigurationTests {

	AnnotationConfigApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void registersJodaModuleAutomatically() {
		this.context.register(JacksonAutoConfiguration.class);
		this.context.refresh();
		Map<String, Module> modules = this.context.getBeansOfType(Module.class);
		assertThat(modules.size(), greaterThanOrEqualTo(1)); // Depends on the JDK
		assertThat(modules.get("jacksonJodaModule"), is(instanceOf(JodaModule.class)));
		ObjectMapper objectMapper = this.context.getBean(ObjectMapper.class);
		assertThat(objectMapper.canSerialize(LocalDateTime.class), is(true));
	}

	@Test
	public void customJacksonModules() throws Exception {
		this.context.register(ModulesConfig.class, JacksonAutoConfiguration.class);
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		@SuppressWarnings({ "unchecked", "unused" })
		ObjectMapper result = verify(mapper).registerModules(
				(Iterable<Module>) argThat(hasItem(this.context.getBean("jacksonModule",
						Module.class))));
	}

	@Test
	public void doubleModuleRegistration() throws Exception {
		this.context.register(DoubleModulesConfig.class,
				HttpMessageConvertersAutoConfiguration.class);
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertEquals("{\"foo\":\"bar\"}", mapper.writeValueAsString(new Foo()));
	}

	@Test
	public void enableSerializationFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.serialization.indent_output:true");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertFalse(SerializationFeature.INDENT_OUTPUT.enabledByDefault());
		assertTrue(mapper.getSerializationConfig().hasSerializationFeatures(
				SerializationFeature.INDENT_OUTPUT.getMask()));
	}

	@Test
	public void disableSerializationFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.serialization.write_dates_as_timestamps:false");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertTrue(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS.enabledByDefault());
		assertFalse(mapper.getSerializationConfig().hasSerializationFeatures(
				SerializationFeature.WRITE_DATES_AS_TIMESTAMPS.getMask()));
	}

	@Test
	public void enableDeserializationFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.deserialization.use_big_decimal_for_floats:true");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertFalse(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.enabledByDefault());
		assertTrue(mapper.getDeserializationConfig().hasDeserializationFeatures(
				DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS.getMask()));
	}

	@Test
	public void disableDeserializationFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.deserialization.fail_on_unknown_properties:false");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertTrue(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.enabledByDefault());
		assertFalse(mapper.getDeserializationConfig().hasDeserializationFeatures(
				DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.getMask()));
	}

	@Test
	public void enableMapperFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.mapper.require_setters_for_getters:true");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertFalse(MapperFeature.REQUIRE_SETTERS_FOR_GETTERS.enabledByDefault());
		assertTrue(mapper.getSerializationConfig().hasMapperFeatures(
				MapperFeature.REQUIRE_SETTERS_FOR_GETTERS.getMask()));
		assertTrue(mapper.getDeserializationConfig().hasMapperFeatures(
				MapperFeature.REQUIRE_SETTERS_FOR_GETTERS.getMask()));
	}

	@Test
	public void disableMapperFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.mapper.use_annotations:false");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertTrue(MapperFeature.USE_ANNOTATIONS.enabledByDefault());
		assertFalse(mapper.getDeserializationConfig().hasMapperFeatures(
				MapperFeature.USE_ANNOTATIONS.getMask()));
		assertFalse(mapper.getSerializationConfig().hasMapperFeatures(
				MapperFeature.USE_ANNOTATIONS.getMask()));
	}

	@Test
	public void enableParserFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.parser.allow_single_quotes:true");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertFalse(JsonParser.Feature.ALLOW_SINGLE_QUOTES.enabledByDefault());
		assertTrue(mapper.getFactory().isEnabled(JsonParser.Feature.ALLOW_SINGLE_QUOTES));
	}

	@Test
	public void disableParserFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.parser.auto_close_source:false");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertTrue(JsonParser.Feature.AUTO_CLOSE_SOURCE.enabledByDefault());
		assertFalse(mapper.getFactory().isEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
	}

	@Test
	public void enableGeneratorFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.generator.write_numbers_as_strings:true");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertFalse(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS.enabledByDefault());
		assertTrue(mapper.getFactory().isEnabled(
				JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS));
	}

	@Test
	public void disableGeneratorFeature() throws Exception {
		this.context.register(JacksonAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.generator.auto_close_target:false");
		this.context.refresh();
		ObjectMapper mapper = this.context.getBean(ObjectMapper.class);
		assertTrue(JsonGenerator.Feature.AUTO_CLOSE_TARGET.enabledByDefault());
		assertFalse(mapper.getFactory()
				.isEnabled(JsonGenerator.Feature.AUTO_CLOSE_TARGET));
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
