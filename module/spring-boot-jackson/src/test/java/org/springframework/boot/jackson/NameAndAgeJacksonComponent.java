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

package org.springframework.boot.jackson;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;

import org.springframework.boot.jackson.types.NameAndAge;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample {@link JacksonComponent @JacksonComponent} used for tests.
 *
 * @author Phillip Webb
 */
@JacksonComponent
public class NameAndAgeJacksonComponent {

	static class Serializer extends ObjectValueSerializer<NameAndAge> {

		@Override
		protected void serializeObject(NameAndAge value, JsonGenerator jgen, SerializationContext context) {
			jgen.writeStringProperty("theName", value.getName());
			jgen.writeNumberProperty("theAge", value.getAge());
		}

	}

	static class Deserializer extends ObjectValueDeserializer<NameAndAge> {

		@Override
		protected NameAndAge deserializeObject(JsonParser jsonParser, DeserializationContext context, JsonNode tree) {
			String name = nullSafeValue(tree.get("name"), String.class);
			Integer age = nullSafeValue(tree.get("age"), Integer.class);
			assertThat(age).isNotNull();
			return NameAndAge.create(name, age);
		}

	}

}
