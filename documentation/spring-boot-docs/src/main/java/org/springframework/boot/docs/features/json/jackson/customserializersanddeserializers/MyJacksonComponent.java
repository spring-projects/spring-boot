/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.json.jackson.customserializersanddeserializers;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;

import org.springframework.boot.jackson.JacksonComponent;

@JacksonComponent
public class MyJacksonComponent {

	public static class Serializer extends ValueSerializer<MyObject> {

		@Override
		public void serialize(MyObject value, JsonGenerator jgen, SerializationContext context) {
			jgen.writeStartObject();
			jgen.writeStringProperty("name", value.getName());
			jgen.writeNumberProperty("age", value.getAge());
			jgen.writeEndObject();
		}

	}

	public static class Deserializer extends ValueDeserializer<MyObject> {

		@Override
		public MyObject deserialize(JsonParser jsonParser, DeserializationContext ctxt) {
			JsonNode tree = jsonParser.readValueAsTree();
			String name = tree.get("name").stringValue();
			int age = tree.get("age").intValue();
			return new MyObject(name, age);
		}

	}

}
