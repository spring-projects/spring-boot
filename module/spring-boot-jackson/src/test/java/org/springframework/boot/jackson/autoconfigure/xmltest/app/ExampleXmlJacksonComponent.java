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

package org.springframework.boot.jackson.autoconfigure.xmltest.app;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationContext;

import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.boot.jackson.ObjectValueDeserializer;
import org.springframework.boot.jackson.ObjectValueSerializer;
import org.springframework.boot.test.autoconfigure.xml.XmlTest;

/**
 * Example {@link JacksonComponent @JacksonComponent} for use with
 * {@link XmlTest @XmlTest} tests.
 *
 * @author Tiziano Basile
 */
@JacksonComponent
public class ExampleXmlJacksonComponent {

	static class Serializer extends ObjectValueSerializer<ExampleCustomObject> {

		@Override
		protected void serializeObject(ExampleCustomObject value, JsonGenerator jgen, SerializationContext context) {
			jgen.writeStringProperty("custom", value.value());
		}

	}

	static class Deserializer extends ObjectValueDeserializer<ExampleCustomObject> {

		@Override
		protected ExampleCustomObject deserializeObject(JsonParser jsonParser, DeserializationContext context,
				JsonNode tree) {
			return new ExampleCustomObject(nullSafeValue(tree.get("custom"), String.class));
		}

	}

}
