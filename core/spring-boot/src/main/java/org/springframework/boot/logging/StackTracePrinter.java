/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Interface that can be used to print the stack trace of a {@link Throwable}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 * @see StandardStackTracePrinter
 */
@FunctionalInterface
public interface StackTracePrinter {

	/**
	 * Return a {@link String} containing the printed stack trace for a given
	 * {@link Throwable}.
	 * @param throwable the throwable that should have its stack trace printed
	 * @return the stack trace string
	 */
	default String printStackTraceToString(Throwable throwable) {
		try {
			StringBuilder out = new StringBuilder(4096);
			printStackTrace(throwable, out);
			return out.toString();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Prints a stack trace for the given {@link Throwable}.
	 * @param throwable the throwable that should have its stack trace printed
	 * @param out the destination to write output
	 * @throws IOException on IO error
	 */
	void printStackTrace(Throwable throwable, Appendable out) throws IOException;

}
