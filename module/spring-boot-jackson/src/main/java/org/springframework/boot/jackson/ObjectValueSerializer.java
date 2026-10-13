/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Helper base class for {@link ValueSerializer} implementations that serialize objects.
 *
 * @param <T> the supported object type
 * @author Phillip Webb
 * @since 4.0.0
 * @see ObjectValueDeserializer
 */
public abstract class ObjectValueSerializer<T> extends ValueSerializer<T> {

	@Override
	public final void serialize(T value, JsonGenerator jgen, SerializationContext context) {
		jgen.writeStartObject();
		serializeObject(value, jgen, context);
		jgen.writeEndObject();
	}

	/**
	 * Serialize JSON content into the value type this serializer handles.
	 * @param value the source value
	 * @param jgen the JSON generator
	 * @param context the serialization context
	 */
	protected abstract void serializeObject(T value, JsonGenerator jgen, SerializationContext context);

}
