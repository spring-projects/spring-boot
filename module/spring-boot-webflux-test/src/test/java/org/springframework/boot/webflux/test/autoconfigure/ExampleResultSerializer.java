/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.boot.jackson.ObjectValueSerializer;

/**
 * {@link ObjectValueSerializer} for {@link ExampleResult}.
 *
 * @author Stephane Nicoll
 */
@JacksonComponent
public class ExampleResultSerializer extends ObjectValueSerializer<ExampleResult> {

	@Override
	protected void serializeObject(ExampleResult item, JsonGenerator jsonWriter, SerializationContext context) {
		jsonWriter.writeStringProperty("identifier", item.id());
	}

}
