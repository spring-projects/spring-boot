/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.structured;

import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.util.LambdaSafe;

/**
 * Base class for {@link StructuredLogFormatter} implementations that generates JSON using
 * a {@link JsonWriter}.
 *
 * @param <E> the log event type
 * @author Phillip Webb
 * @since 3.4.0
 */
public abstract class JsonWriterStructuredLogFormatter<E> implements StructuredLogFormatter<E> {

	private final JsonWriter<E> jsonWriter;

	/**
	 * Create a new {@link JsonWriterStructuredLogFormatter} instance with the given
	 * members.
	 * @param members a consumer, which should configure the members
	 * @param customizer an optional customizer to apply
	 */
	protected JsonWriterStructuredLogFormatter(Consumer<Members<E>> members,
			StructuredLoggingJsonMembersCustomizer<?> customizer) {
		this(JsonWriter.of(customized(members, customizer)).withNewLineAtEnd());
	}

	private static <E> Consumer<Members<E>> customized(Consumer<Members<E>> members,
			StructuredLoggingJsonMembersCustomizer<?> customizer) {
		return (customizer != null) ? members.andThen(customizeWith(customizer)) : members;
	}

	@SuppressWarnings("unchecked")
	private static <E> Consumer<Members<E>> customizeWith(StructuredLoggingJsonMembersCustomizer<?> customizer) {
		return (members) -> LambdaSafe.callback(StructuredLoggingJsonMembersCustomizer.class, customizer, members)
			.invoke((instance) -> instance.customize(members));
	}

	/**
	 * Create a new {@link JsonWriterStructuredLogFormatter} instance with the given
	 * {@link JsonWriter}.
	 * @param jsonWriter the {@link JsonWriter}
	 */
	protected JsonWriterStructuredLogFormatter(JsonWriter<E> jsonWriter) {
		this.jsonWriter = jsonWriter;
	}

	@Override
	public String format(E event) {
		return this.jsonWriter.writeToString(event);
	}

	@Override
	public byte[] formatAsBytes(E event, Charset charset) {
		return this.jsonWriter.write(event).toByteArray(charset);
	}

}
