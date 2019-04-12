package org.springframework.boot.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * Sample {@link JsonComponent} used for tests.
 *
 * @author Paul Aly
 */
@JsonComponent(handle = JsonComponent.Handle.KEYS, handleClasses = NameAndAge.class)
public class NameAndAgeJsonKeyComponent {

	public static class Serializer extends JsonSerializer<NameAndAge> {

		@Override
		public void serialize(NameAndAge value, JsonGenerator jgen,
				SerializerProvider serializers) throws IOException {
			jgen.writeFieldName(value.asKey());
		}

	}

	public static class Deserializer extends KeyDeserializer {

		@Override
		public NameAndAge deserializeKey(String key, DeserializationContext ctxt)
				throws IOException {
			String[] keys = key.split("is");
			return new NameAndAge(keys[0].trim(), Integer.valueOf(keys[1].trim()));
		}

	}

}
