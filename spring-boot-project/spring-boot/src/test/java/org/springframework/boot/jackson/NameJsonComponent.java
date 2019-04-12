package org.springframework.boot.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

@JsonComponent
public class NameJsonComponent {

	@JsonComponent(handleClasses = NameAndCareer.class)
	public static class NameSerializer extends JsonObjectSerializer<Name> {

		@Override
		protected void serializeObject(Name value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeStringField("name", value.getName());
		}
	}
}
