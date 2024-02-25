/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.features.json.jackson.customserializersanddeserializers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson.JsonComponent;

/**
 * MyJsonComponent class.
 */
@JsonComponent
public class MyJsonComponent {

	/**
	 * Serializer class.
	 */
	public static class Serializer extends JsonSerializer<MyObject> {

		/**
		 * Serializes a MyObject instance into JSON format.
		 * @param value The MyObject instance to be serialized.
		 * @param jgen The JsonGenerator used for writing JSON content.
		 * @param serializers The SerializerProvider used for accessing serializers.
		 * @throws IOException If an I/O error occurs during serialization.
		 */
		@Override
		public void serialize(MyObject value, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
			jgen.writeStartObject();
			jgen.writeStringField("name", value.getName());
			jgen.writeNumberField("age", value.getAge());
			jgen.writeEndObject();
		}

	}

	/**
	 * Deserializer class.
	 */
	public static class Deserializer extends JsonDeserializer<MyObject> {

		/**
		 * Deserializes a JSON object into a MyObject instance.
		 * @param jsonParser The JSON parser used to read the JSON object.
		 * @param ctxt The deserialization context.
		 * @return A MyObject instance representing the deserialized JSON object.
		 * @throws IOException If an I/O error occurs while reading the JSON object.
		 */
		@Override
		public MyObject deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {
			ObjectCodec codec = jsonParser.getCodec();
			JsonNode tree = codec.readTree(jsonParser);
			String name = tree.get("name").textValue();
			int age = tree.get("age").intValue();
			return new MyObject(name, age);
		}

	}

}
