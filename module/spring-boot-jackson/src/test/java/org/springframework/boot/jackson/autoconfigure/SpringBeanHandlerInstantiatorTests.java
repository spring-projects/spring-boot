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

package org.springframework.boot.jackson.autoconfigure;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.annotation.JsonTypeIdResolver;
import tools.jackson.databind.annotation.JsonTypeResolver;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.NamedType;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBeanHandlerInstantiator}. Adapted from Spring Framework's
 * <a href=
 * "https://github.com/spring-projects/spring-framework/blob/b246fc881b2bc83daea0a333f1c2fea7212e569d/spring-web/src/test/java/org/springframework/http/support/JacksonHandlerInstantiatorTests.java">JacksonHandlerInstantiatorTests</a>.
 *
 * @author Sebastien Deleuze
 * @author Andy Wilkinson
 */
class SpringBeanHandlerInstantiatorTests {

	@Test
	void autowiredSerializer() {
		withHandlerInstantiator((jsonMapper) -> {
			User user = new User("bob");
			String json = jsonMapper.writeValueAsString(user);
			assertThat(json).isEqualTo("{\"username\":\"BOB\"}");
		});
	}

	@Test
	void autowiredDeserializer() {
		withHandlerInstantiator((jsonMapper) -> {
			String json = "{\"username\":\"bob\"}";
			User user = jsonMapper.readValue(json, User.class);
			assertThat(user.getUsername()).isEqualTo("BOB");
		});
	}

	@Test
	void autowiredKeyDeserializer() {
		withHandlerInstantiator((jsonMapper) -> {
			String json = "{\"credentials\":{\"bob\":\"admin\"}}";
			SecurityRegistry registry = jsonMapper.readValue(json, SecurityRegistry.class);
			assertThat(registry.getCredentials()).containsKey("BOB");
			assertThat(registry.getCredentials()).doesNotContainKey("bob");
		});
	}

	@Test
	void applicationContextAwaretypeResolverBuilder() {
		withHandlerInstantiator((jsonMapper) -> {
			jsonMapper.writeValueAsString(new Group());
			assertThat(CustomTypeResolverBuilder.autowiredFieldInitialized).isTrue();
		});
	}

	@Test
	void applicationContextAwareTypeIdResolver() {
		withHandlerInstantiator((jsonMapper) -> {
			jsonMapper.writeValueAsString(new Group());
			assertThat(CustomTypeIdResolver.autowiredFieldInitialized).isTrue();
		});
	}

	private void withHandlerInstantiator(Consumer<JsonMapper> consumer) {
		new ApplicationContextRunner().withBean(Capitalizer.class)
			.run((context) -> consumer.accept(JsonMapper.builder()
				.handlerInstantiator(new SpringBeanHandlerInstantiator(context.getBeanFactory()))
				.build()));
	}

	static class UserDeserializer extends ValueDeserializer<User> {

		@Autowired
		private Capitalizer capitalizer;

		@Override
		public User deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
			JsonNode node = jsonParser.readValueAsTree();
			return new User(this.capitalizer.capitalize(node.get("username").asString()));
		}

	}

	static class UserSerializer extends ValueSerializer<User> {

		@Autowired
		private Capitalizer capitalizer;

		@Override
		public void serialize(User user, JsonGenerator jsonGenerator, SerializationContext serializationContext) {

			jsonGenerator.writeStartObject();
			jsonGenerator.writeStringProperty("username", this.capitalizer.capitalize(user.getUsername()));
			jsonGenerator.writeEndObject();
		}

	}

	static class UpperCaseKeyDeserializer extends KeyDeserializer {

		@Autowired
		private Capitalizer capitalizer;

		@Override
		public Object deserializeKey(String key, DeserializationContext context) {
			return this.capitalizer.capitalize(key);
		}

	}

	static class CustomTypeResolverBuilder extends StdTypeResolverBuilder {

		@Autowired
		private Capitalizer capitalizer;

		static boolean autowiredFieldInitialized;

		@Override
		public TypeSerializer buildTypeSerializer(SerializationContext serializationContext, JavaType baseType,
				Collection<NamedType> subtypes) {

			autowiredFieldInitialized = (this.capitalizer != null);
			return super.buildTypeSerializer(serializationContext, baseType, subtypes);
		}

		@Override
		public TypeDeserializer buildTypeDeserializer(DeserializationContext deserializationContext, JavaType baseType,
				Collection<NamedType> subtypes) {

			return super.buildTypeDeserializer(deserializationContext, baseType, subtypes);
		}

	}

	static class CustomTypeIdResolver implements TypeIdResolver {

		@Autowired
		private Capitalizer capitalizer;

		static boolean autowiredFieldInitialized;

		@Override
		public String idFromValueAndType(DatabindContext ctxt, Object o, Class<?> type) {
			return type.getClass().getName();
		}

		@Override
		public JsonTypeInfo.Id getMechanism() {
			return JsonTypeInfo.Id.CUSTOM;
		}

		@Override
		public String idFromValue(DatabindContext databindContext, Object value) {
			autowiredFieldInitialized = (this.capitalizer != null);
			return value.getClass().getName();
		}

		@Override
		public void init(JavaType type) {
		}

		@Override
		public @Nullable String idFromBaseType(DatabindContext ctxt) {
			return null;
		}

		@Override
		public @Nullable JavaType typeFromId(DatabindContext context, String id) {
			return null;
		}

		@Override
		public @Nullable String getDescForKnownTypeIds() {
			return null;
		}

	}

	@JsonDeserialize(using = UserDeserializer.class)
	@JsonSerialize(using = UserSerializer.class)
	public static class User {

		private final String username;

		User(String username) {
			this.username = username;
		}

		public String getUsername() {
			return this.username;
		}

	}

	public static class SecurityRegistry {

		@JsonDeserialize(keyUsing = UpperCaseKeyDeserializer.class)
		private Map<String, String> credentials = new HashMap<>();

		public void addCredential(String username, String credential) {
			this.credentials.put(username, credential);
		}

		public Map<String, String> getCredentials() {
			return this.credentials;
		}

	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
	@JsonTypeResolver(CustomTypeResolverBuilder.class)
	@JsonTypeIdResolver(CustomTypeIdResolver.class)
	public static class Group {

		public String getType() {
			return Group.class.getName();
		}

	}

	static class Capitalizer {

		String capitalize(String text) {
			return text.toUpperCase(Locale.ROOT);
		}

	}

}
