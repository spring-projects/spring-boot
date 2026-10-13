/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.json.jackson.customserializersanddeserializers

import tools.jackson.core.JsonGenerator
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.ValueSerializer

import org.springframework.boot.jackson.JacksonComponent

@JacksonComponent
class MyJacksonComponent {

	class Serializer : ValueSerializer<MyObject>() {
		override fun serialize(value: MyObject, jgen: JsonGenerator, serializers: SerializationContext) {
			jgen.writeStartObject()
			jgen.writeStringProperty("name", value.name)
			jgen.writeNumberProperty("age", value.age)
			jgen.writeEndObject()
		}
	}

	class Deserializer : ValueDeserializer<MyObject>() {
		override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): MyObject {
			val tree = jsonParser.readValueAsTree<JsonNode>()
			val name = tree["name"].stringValue()
			val age = tree["age"].intValue()
			return MyObject(name, age)
		}
	}

}

