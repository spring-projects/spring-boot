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

package org.springframework.boot.docs.features.json.jackson.customserializersanddeserializers.`object`

import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationContext

import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.boot.jackson.ObjectValueDeserializer
import org.springframework.boot.jackson.ObjectValueSerializer

@JacksonComponent
class MyJacksonComponent {

	class Serializer : ObjectValueSerializer<MyObject>() {
		override fun serializeObject(value: MyObject, jgen: JsonGenerator, context: SerializationContext) {
			jgen.writeStringProperty("name", value.name)
			jgen.writeNumberProperty("age", value.age)
		}
	}

	class Deserializer : ObjectValueDeserializer<MyObject>() {
		override fun deserializeObject(jsonParser: JsonParser, context: DeserializationContext,
				tree: JsonNode): MyObject {
			val name = nullSafeValue(tree["name"], String::class.java) ?: throw IllegalStateException("name is null")
			val age = nullSafeValue(tree["age"], Int::class.java) ?: throw IllegalStateException("age is null")
			return MyObject(name, age)
		}
	}

}

