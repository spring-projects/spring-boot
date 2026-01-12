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
