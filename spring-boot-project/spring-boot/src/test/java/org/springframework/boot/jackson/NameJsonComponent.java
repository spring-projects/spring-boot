package org.springframework.boot.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

@JsonComponent(handle = JsonComponent.Handle.KEYS,
		handleClasses = { Name.class, NameAndCareer.class, NameAndAge.class })
public class NameJsonComponent {

	@JsonComponent(handleClasses = NameAndCareer.class)
	public static class NameSerializer extends JsonObjectSerializer<Name> {

		@Override
		protected void serializeObject(Name value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException {
			jgen.writeStringField("name", value.getName());
		}

	}

	@JsonComponent(handleClasses = NameAndCareer.class)
	public static class NameDeserializer extends JsonObjectDeserializer<Name> {

		@Override
		protected Name deserializeObject(JsonParser jsonParser,
				DeserializationContext context, ObjectCodec codec, JsonNode tree)
				throws IOException {
			String name = nullSafeValue(tree.get("name"), String.class);
			String career = nullSafeValue(tree.get("career"), String.class);
			return new NameAndCareer(name, career);
		}

	}

}
