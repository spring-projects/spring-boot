/*
 * Copyright 2012-present the original author or authors.
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

