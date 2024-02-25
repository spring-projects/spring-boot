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

package org.springframework.boot.cli.util;

/**
 * Simple logger used by the CLI.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public abstract class Log {

	private static LogListener listener;

	/**
	 * Prints the given message to the console and notifies the listener if available.
	 * @param message the message to be printed
	 */
	public static void info(String message) {
		System.out.println(message);
		if (listener != null) {
			listener.info(message);
		}
	}

	/**
	 * Prints the given message to the console and notifies the listener if available.
	 * @param message the message to be printed
	 */
	public static void infoPrint(String message) {
		System.out.print(message);
		if (listener != null) {
			listener.infoPrint(message);
		}
	}

	/**
	 * Prints the error message to the standard error stream and notifies the listener, if
	 * available.
	 * @param message the error message to be printed
	 */
	public static void error(String message) {
		System.err.println(message);
		if (listener != null) {
			listener.error(message);
		}
	}

	/**
	 * Prints the stack trace of the given exception to the standard error stream. If a
	 * listener is registered, it also notifies the listener about the error.
	 * @param ex the exception to be handled
	 */
	public static void error(Exception ex) {
		ex.printStackTrace(System.err);
		if (listener != null) {
			listener.error(ex);
		}
	}

	/**
	 * Sets the LogListener for the Log class.
	 * @param listener the LogListener to be set
	 */
	static void setListener(LogListener listener) {
		Log.listener = listener;
	}

}
