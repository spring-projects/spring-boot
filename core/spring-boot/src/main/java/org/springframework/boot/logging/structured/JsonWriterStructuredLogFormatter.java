/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.structured;

import java.nio.charset.Charset;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

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
			@Nullable StructuredLoggingJsonMembersCustomizer<?> customizer) {
		this(JsonWriter.of(customized(members, customizer)).withNewLineAtEnd());
	}

	private static <E> Consumer<Members<E>> customized(Consumer<Members<E>> members,
			@Nullable StructuredLoggingJsonMembersCustomizer<?> customizer) {
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
