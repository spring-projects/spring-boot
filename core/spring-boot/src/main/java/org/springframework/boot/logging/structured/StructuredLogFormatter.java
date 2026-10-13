/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.structured;

import java.nio.charset.Charset;

import org.springframework.boot.logging.StackTracePrinter;
import org.springframework.core.env.Environment;

/**
 * Formats a log event to a structured log message.
 * <p>
 * Implementing classes can declare the following parameter types in the constructor:
 * <ul>
 * <li>{@link Environment}</li>
 * <li>{@link StructuredLoggingJsonMembersCustomizer}</li>
 * <li>{@link StructuredLoggingJsonMembersCustomizer.Builder}</li>
 * <li>{@link StackTracePrinter} (may be {@code null})</li>
 * <li>{@link ContextPairs}</li>
 * </ul>
 * When using Logback, implementing classes can also use the following parameter types in
 * the constructor:
 * <ul>
 * <li>{@code ch.qos.logback.classic.pattern.ThrowableProxyConverter}</li>
 * </ul>
 *
 * @param <E> the log event type
 * @author Moritz Halbritter
 * @since 3.4.0
 */
@FunctionalInterface
public interface StructuredLogFormatter<E> {

	/**
	 * Formats the given log event to a String.
	 * @param event the log event to write
	 * @return the formatted log event String
	 */
	String format(E event);

	/**
	 * Formats the given log event to a byte array.
	 * @param event the log event to write
	 * @param charset the charset
	 * @return the formatted log event bytes
	 */
	default byte[] formatAsBytes(E event, Charset charset) {
		return format(event).getBytes(charset);
	}

}
