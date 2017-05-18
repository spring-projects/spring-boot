/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.json;

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
 * Example {@link JsonComponent} for use with {@link JsonTest @JsonTest} tests.
 *
 * @author Phillip Webb
 */
@JsonComponent
public class ExampleJsonComponent {

	public static class Serializer extends JsonObjectSerializer<ExampleCustomObject> {

		@Override
		protected void serializeObject(ExampleCustomObject value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException {
			jgen.writeStringField("value", value.toString());
		}

	}

	public static class Deserializer extends JsonObjectDeserializer<ExampleCustomObject> {

		@Override
		protected ExampleCustomObject deserializeObject(JsonParser jsonParser,
				DeserializationContext context, ObjectCodec codec, JsonNode tree)
						throws IOException {
			return new ExampleCustomObject(
					nullSafeValue(tree.get("value"), String.class));
		}

	}

}
