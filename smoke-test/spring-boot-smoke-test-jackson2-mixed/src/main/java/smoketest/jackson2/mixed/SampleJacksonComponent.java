/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.mixed;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import org.springframework.boot.jackson.JacksonComponent;

@JacksonComponent
public class SampleJacksonComponent {

	public static class Serializer extends ValueSerializer<Name> {

		@Override
		public void serialize(Name value, JsonGenerator gen, SerializationContext ctxt) throws JacksonException {
			gen.writeString("JACKSON:%s:%s".formatted(value.first(), value.last()));
		}

	}

}
