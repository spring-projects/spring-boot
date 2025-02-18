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

package org.springframework.boot.docs.features.json.jackson.customserializersanddeserializers.`object`

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import org.springframework.boot.jackson.JsonComponent
import org.springframework.boot.jackson.JsonObjectDeserializer
import org.springframework.boot.jackson.JsonObjectSerializer
import java.io.IOException

@JsonComponent
class MyJsonComponent {

	class Serializer : JsonObjectSerializer<MyObject>() {
		@Throws(IOException::class)
		override fun serializeObject(value: MyObject, jgen: JsonGenerator, provider: SerializerProvider) {
			jgen.writeStringField("name", value.name)
			jgen.writeNumberField("age", value.age)
		}
	}

	class Deserializer : JsonObjectDeserializer<MyObject>() {
		@Throws(IOException::class)
		override fun deserializeObject(jsonParser: JsonParser, context: DeserializationContext,
				codec: ObjectCodec, tree: JsonNode): MyObject {
			val name = nullSafeValue(tree["name"], String::class.java)
			val age = nullSafeValue(tree["age"], Int::class.java)
			return MyObject(name, age)
		}
	}

}
