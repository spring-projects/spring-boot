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
