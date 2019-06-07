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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Sample {@link JsonComponent} used for tests.
 *
 * @author Paul Aly
 */
@JsonComponent(type = NameAndCareer.class)
public class NameAndCareerJsonComponent {

	public static class Serializer extends JsonObjectSerializer<Name> {

		@Override
		protected void serializeObject(Name value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeStringField("name", value.getName());
		}

	}

	public static class Deserializer extends JsonObjectDeserializer<Name> {

		@Override
		protected Name deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec,
				JsonNode tree) throws IOException {
			String name = nullSafeValue(tree.get("name"), String.class);
			String career = nullSafeValue(tree.get("career"), String.class);
			return new NameAndCareer(name, career);
		}

	}

}
