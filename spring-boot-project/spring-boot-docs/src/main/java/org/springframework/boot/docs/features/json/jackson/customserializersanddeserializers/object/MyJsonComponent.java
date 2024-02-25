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

package org.springframework.boot.docs.features.json.jackson.customserializersanddeserializers.object;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson.JsonComponent;
import org.springframework.boot.jackson.JsonObjectDeserializer;
import org.springframework.boot.jackson.JsonObjectSerializer;

/**
 * MyJsonComponent class.
 */
@JsonComponent
public class MyJsonComponent {

	/**
	 * Serializer class.
	 */
	public static class Serializer extends JsonObjectSerializer<MyObject> {

		/**
		 * Serializes a MyObject instance into a JSON representation.
		 * @param value the MyObject instance to be serialized
		 * @param jgen the JsonGenerator used for writing JSON content
		 * @param provider the SerializerProvider used for accessing serialization
		 * functionality
		 * @throws IOException if an I/O error occurs during serialization
		 */
		@Override
		protected void serializeObject(MyObject value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException {
			jgen.writeStringField("name", value.getName());
			jgen.writeNumberField("age", value.getAge());
		}

	}

	/**
	 * Deserializer class.
	 */
	public static class Deserializer extends JsonObjectDeserializer<MyObject> {

		/**
		 * Deserializes a JSON object into a MyObject instance.
		 * @param jsonParser The JSON parser used for parsing the JSON data.
		 * @param context The deserialization context.
		 * @param codec The object codec used for reading JSON values.
		 * @param tree The JSON tree representing the object to be deserialized.
		 * @return A MyObject instance created from the deserialized JSON data.
		 * @throws IOException If an I/O error occurs during deserialization.
		 */
		@Override
		protected MyObject deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec,
				JsonNode tree) throws IOException {
			String name = nullSafeValue(tree.get("name"), String.class);
			int age = nullSafeValue(tree.get("age"), Integer.class);
			return new MyObject(name, age);
		}

	}

}
