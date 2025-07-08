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
