/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.mixed;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson2.JsonComponent;

@JsonComponent
@SuppressWarnings("removal")
public class SampleJsonComponent {

	public static class Serializer extends JsonSerializer<Name> {

		@Override
		public void serialize(Name value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			gen.writeString("JACKSON2:%s:%s".formatted(value.first(), value.last()));
		}

	}

}
