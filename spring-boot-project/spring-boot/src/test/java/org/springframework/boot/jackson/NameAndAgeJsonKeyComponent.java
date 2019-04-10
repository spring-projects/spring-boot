package org.springframework.boot.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdKeyDeserializer;

import java.io.IOException;

@JsonComponent(handle = JsonComponent.Handle.KEYS)
public class NameAndAgeJsonKeyComponent {

	public static class Serializer extends JsonSerializer<NameAndAge> {

		@Override
		public void serialize(NameAndAge value, JsonGenerator jgen,
				SerializerProvider serializers) throws IOException {
			jgen.writeFieldName(value.asKey());
		}

	}

	public static class Deserializer extends StdKeyDeserializer {

		protected Deserializer() {
			super(TYPE_CLASS, NameAndAge.class);
		}

		@Override
		public NameAndAge deserializeKey(String key, DeserializationContext ctxt)
				throws IOException {
			String[] keys = key.split("is");
			return new NameAndAge(keys[0].trim(), Integer.valueOf(keys[1].trim()));
		}

	}

}
