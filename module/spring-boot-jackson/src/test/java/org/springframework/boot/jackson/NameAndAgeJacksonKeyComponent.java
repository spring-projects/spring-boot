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
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.KeyDeserializer;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import org.springframework.boot.jackson.types.NameAndAge;

/**
 * Sample {@link JacksonComponent @JacksonComponent} used for tests.
 *
 * @author Paul Aly
 */
@JacksonComponent(type = NameAndAge.class, scope = JacksonComponent.Scope.KEYS)
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
