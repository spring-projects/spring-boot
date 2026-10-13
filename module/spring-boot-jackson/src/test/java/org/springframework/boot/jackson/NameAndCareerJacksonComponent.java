/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;

import org.springframework.boot.jackson.types.Name;
import org.springframework.boot.jackson.types.NameAndCareer;

/**
 * Sample {@link JacksonComponent @JacksonComponent} used for tests.
 *
 * @author Paul Aly
 */
@JacksonComponent(type = NameAndCareer.class)
public class NameAndCareerJacksonComponent {

	static class Serializer extends ObjectValueSerializer<Name> {

		@Override
		protected void serializeObject(Name value, JsonGenerator jgen, SerializationContext context) {
			jgen.writeStringProperty("name", value.getName());
		}

	}

	static class Deserializer extends ObjectValueDeserializer<Name> {

		@Override
		protected Name deserializeObject(JsonParser jsonParser, DeserializationContext context, JsonNode tree) {
			String name = nullSafeValue(tree.get("name"), String.class);
			String career = nullSafeValue(tree.get("career"), String.class);
			return new NameAndCareer(name, career);
		}

	}

}
