/*
 * Copyright 2012-present the original author or authors.
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
