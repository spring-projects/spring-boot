/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson2.types.NameAndAge;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sample {@link JsonComponent @JsonComponent} used for tests.
 *
 * @author Phillip Webb
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
@JsonComponent
public class NameAndAgeJsonComponent {

	static class Serializer extends JsonObjectSerializer<NameAndAge> {

		@Override
		protected void serializeObject(NameAndAge value, JsonGenerator jgen, SerializerProvider provider)
				throws IOException {
			jgen.writeStringField("name", value.getName());
			jgen.writeNumberField("age", value.getAge());
		}

	}

	static class Deserializer extends JsonObjectDeserializer<NameAndAge> {

		@Override
		protected NameAndAge deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec,
				JsonNode tree) throws IOException {
			String name = nullSafeValue(tree.get("name"), String.class);
			Integer age = nullSafeValue(tree.get("age"), Integer.class);
			assertThat(age).isNotNull();
			return new NameAndAge(name, age);
		}

	}

}
