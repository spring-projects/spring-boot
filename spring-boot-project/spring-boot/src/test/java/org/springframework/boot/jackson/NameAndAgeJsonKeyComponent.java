/*
 * Copyright 2012-2019 the original author or authors.
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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Sample {@link JsonComponent} used for tests.
 *
 * @author Paul Aly
 */
@JsonComponent(type = NameAndAge.class, scope = JsonComponent.Scope.KEYS)
public class NameAndAgeJsonKeyComponent {

	public static class Serializer extends JsonSerializer<NameAndAge> {

		@Override
		public void serialize(NameAndAge value, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
			jgen.writeFieldName(value.asKey());
		}

	}

	public static class Deserializer extends KeyDeserializer {

		@Override
		public NameAndAge deserializeKey(String key, DeserializationContext ctxt) throws IOException {
			String[] keys = key.split("is");
			return new NameAndAge(keys[0].trim(), Integer.valueOf(keys[1].trim()));
		}

	}

}
