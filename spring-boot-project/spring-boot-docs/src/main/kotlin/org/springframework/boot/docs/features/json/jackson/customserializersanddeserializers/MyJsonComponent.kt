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

package org.springframework.boot.docs.features.json.jackson.customserializersanddeserializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException

@JsonComponent
class MyJsonComponent {

	class Serializer : JsonSerializer<MyObject>() {
		@Throws(IOException::class)
		override fun serialize(value: MyObject, jgen: JsonGenerator, serializers: SerializerProvider) {
			jgen.writeStartObject()
			jgen.writeStringField("name", value.name)
			jgen.writeNumberField("age", value.age)
			jgen.writeEndObject()
		}
	}

	class Deserializer : JsonDeserializer<MyObject>() {
		@Throws(IOException::class, JsonProcessingException::class)
		override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): MyObject {
			val codec = jsonParser.codec
			val tree = codec.readTree<JsonNode>(jsonParser)
			val name = tree["name"].textValue()
			val age = tree["age"].intValue()
			return MyObject(name, age)
		}
	}

}