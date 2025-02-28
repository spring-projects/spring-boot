/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Helper base class for {@link JsonSerializer} implementations that serialize objects.
 *
 * @param <T> the supported object type
 * @author Phillip Webb
 * @since 1.4.0
 * @see JsonObjectDeserializer
 */
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
