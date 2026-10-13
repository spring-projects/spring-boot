/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson2.JsonComponent;
import org.springframework.boot.jackson2.JsonObjectSerializer;

/**
 * {@link JsonObjectSerializer} for {@link ExampleResult}.
 *
 * @author Stephane Nicoll
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@JsonComponent
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public class ExampleResult2Serializer extends JsonObjectSerializer<ExampleResult2> {

	@Override
	protected void serializeObject(ExampleResult2 item, JsonGenerator jgen, SerializerProvider provider)
			throws IOException {
		jgen.writeStringField("identifier2", item.id());
	}

}
