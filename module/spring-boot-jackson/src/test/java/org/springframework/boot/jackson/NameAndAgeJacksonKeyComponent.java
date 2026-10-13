/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import org.springframework.boot.jackson.JacksonComponent.Scope;
import org.springframework.boot.jackson.types.NameAndAge;

/**
 * Sample {@link JacksonComponent @JacksonComponent} used for tests.
 *
 * @author Paul Aly
 */
@JacksonComponent(type = NameAndAge.class, scope = Scope.KEYS)
public class NameAndAgeJacksonKeyComponent {

	static class Serializer extends ValueSerializer<NameAndAge> {

		@Override
		public void serialize(NameAndAge value, JsonGenerator jgen, SerializationContext serializers) {
			jgen.writeName(value.asKey());
		}

	}

	static class Deserializer extends KeyDeserializer {

		@Override
		public NameAndAge deserializeKey(String key, DeserializationContext ctxt) {
			String[] keys = key.split("is");
			return NameAndAge.create(keys[0].trim(), Integer.parseInt(keys[1].trim()));
		}

	}

}
