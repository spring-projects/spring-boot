/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson2.types.NameAndAge;

/**
 * Sample {@link JsonComponent @JsonComponent} used for tests.
 *
 * @author Paul Aly
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
@JsonComponent(type = NameAndAge.class, scope = org.springframework.boot.jackson2.JsonComponent.Scope.KEYS)
public class NameAndAgeJsonKeyComponent {

	@Deprecated(since = "4.0.0", forRemoval = true)
	static class Serializer extends JsonSerializer<NameAndAge> {

		@Deprecated(since = "4.0.0", forRemoval = true)
		@Override
		public void serialize(NameAndAge value, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
			jgen.writeFieldName(value.asKey());
		}

	}

	@Deprecated(since = "4.0.0", forRemoval = true)
	static class Deserializer extends KeyDeserializer {

		@Deprecated(since = "4.0.0", forRemoval = true)
		@Override
		public NameAndAge deserializeKey(String key, DeserializationContext ctxt) throws IOException {
			String[] keys = key.split("is");
			return new NameAndAge(keys[0].trim(), Integer.parseInt(keys[1].trim()));
		}

	}

}
