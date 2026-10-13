/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import org.springframework.boot.jackson.ObjectValueSerializer;

/**
 * Helper base class for {@link JsonSerializer} implementations that serialize objects.
 *
 * @param <T> the supported object type
 * @author Phillip Webb
 * @since 4.0.0
 * @see JsonObjectDeserializer
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3 and
 * {@link ObjectValueSerializer}
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public abstract class JsonObjectSerializer<T> extends JsonSerializer<T> {

	@Override
	public final void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
		try {
			jgen.writeStartObject();
			serializeObject(value, jgen, provider);
			jgen.writeEndObject();
		}
		catch (Exception ex) {
			if (ex instanceof IOException ioException) {
				throw ioException;
			}
			throw new JsonMappingException(jgen, "Object serialize error", ex);
		}
	}

	/**
	 * Serialize JSON content into the value type this serializer handles.
	 * @param value the source value
	 * @param jgen the JSON generator
	 * @param provider the serializer provider
	 * @throws IOException on error
	 */
	protected abstract void serializeObject(T value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException;

}
