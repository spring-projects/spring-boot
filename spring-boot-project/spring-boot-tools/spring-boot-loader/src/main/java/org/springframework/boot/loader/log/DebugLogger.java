/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.log;

/**
 * Simple logger class used for {@link System#err} debugging.
 *
 * @author Phillip Webb
 * @since 3.2.0
 */
public abstract sealed class DebugLogger {

	private static final String ENABLED_PROPERTY = "loader.debug";

	private static final DebugLogger disabled;
	static {
		disabled = Boolean.getBoolean(ENABLED_PROPERTY) ? null : new DisabledDebugLogger();
	}

	/**
	 * Log a message.
	 * @param message the message to log
	 */
	public abstract void log(String message);

	/**
	 * Log a formatted message.
	 * @param message the message to log
	 * @param arg1 the first format argument
	 */
	public abstract void log(String message, Object arg1);

	/**
	 * Log a formatted message.
	 * @param message the message to log
	 * @param arg1 the first format argument
	 * @param arg2 the second format argument
	 */
	public abstract void log(String message, Object arg1, Object arg2);

	/**
	 * Log a formatted message.
	 * @param message the message to log
	 * @param arg1 the first format argument
	 * @param arg2 the second format argument
	 * @param arg3 the third format argument
	 */
	public abstract void log(String message, Object arg1, Object arg2, Object arg3);

	/**
	 * Log a formatted message.
	 * @param message the message to log
	 * @param arg1 the first format argument
	 * @param arg2 the second format argument
	 * @param arg3 the third format argument
	 * @param arg4 the fourth format argument
	 */
	public abstract void log(String message, Object arg1, Object arg2, Object arg3, Object arg4);

	/**
	 * Get a {@link DebugLogger} to log messages for the given source class.
	 * @param sourceClass the source class
	 * @return a {@link DebugLogger} instance
	 */
	public static DebugLogger get(Class<?> sourceClass) {
		return (disabled != null) ? disabled : new SystemErrDebugLogger(sourceClass);
	}

	/**
	 * {@link DebugLogger} used for disabled logging that does nothing.
	 */
	private static final class DisabledDebugLogger extends DebugLogger {

		/**
         * Logs the specified message.
         *
         * @param message the message to be logged
         */
        @Override
		public void log(String message) {
		}

		/**
         * Logs a message with an argument.
         *
         * @param message the message to be logged
         * @param arg1 the argument to be included in the log message
         */
        @Override
		public void log(String message, Object arg1) {
		}

		/**
         * Logs a message with two arguments.
         *
         * @param message the message to be logged
         * @param arg1 the first argument
         * @param arg2 the second argument
         */
        @Override
		public void log(String message, Object arg1, Object arg2) {
		}

		/**
         * Logs a message with three arguments.
         *
         * @param message the message to be logged
         * @param arg1 the first argument
         * @param arg2 the second argument
         * @param arg3 the third argument
         */
        @Override
		public void log(String message, Object arg1, Object arg2, Object arg3) {
		}

		/**
         * Logs a message with four additional arguments.
         *
         * @param message the message to be logged
         * @param arg1 the first argument
         * @param arg2 the second argument
         * @param arg3 the third argument
         * @param arg4 the fourth argument
         */
        @Override
		public void log(String message, Object arg1, Object arg2, Object arg3, Object arg4) {
		}

	}

	/**
	 * {@link DebugLogger} that prints messages to {@link System#err}.
	 */
	private static final class SystemErrDebugLogger extends DebugLogger {

		private final String prefix;

		/**
         * Constructs a new SystemErrDebugLogger object with the specified source class.
         * 
         * @param sourceClass the class from which the logger is being instantiated
         */
        SystemErrDebugLogger(Class<?> sourceClass) {
			this.prefix = "LOADER: " + sourceClass + " : ";
		}

		/**
         * Logs the specified message to the standard error output.
         * 
         * @param message the message to be logged
         */
        @Override
		public void log(String message) {
			print(message);
		}

		/**
         * Logs a message with an argument to the standard error output stream.
         * 
         * @param message the message to be logged
         * @param arg1 the argument to be formatted in the message
         */
        @Override
		public void log(String message, Object arg1) {
			print(message.formatted(arg1));
		}

		/**
         * Logs a message with two arguments to the standard error output.
         * 
         * @param message the message to be logged
         * @param arg1 the first argument to be formatted in the message
         * @param arg2 the second argument to be formatted in the message
         */
        @Override
		public void log(String message, Object arg1, Object arg2) {
			print(message.formatted(arg1, arg2));
		}

		/**
         * Logs a message with three arguments to the standard error output stream.
         * 
         * @param message the message to be logged
         * @param arg1 the first argument to be formatted in the message
         * @param arg2 the second argument to be formatted in the message
         * @param arg3 the third argument to be formatted in the message
         */
        @Override
		public void log(String message, Object arg1, Object arg2, Object arg3) {
			print(message.formatted(arg1, arg2, arg3));
		}

		/**
         * Logs a message with four arguments to the standard error output stream.
         * 
         * @param message the message to be logged
         * @param arg1 the first argument to be formatted in the message
         * @param arg2 the second argument to be formatted in the message
         * @param arg3 the third argument to be formatted in the message
         * @param arg4 the fourth argument to be formatted in the message
         */
        @Override
		public void log(String message, Object arg1, Object arg2, Object arg3, Object arg4) {
			print(message.formatted(arg1, arg2, arg3, arg4));
		}

		/**
         * Prints the specified message to the standard error stream with a prefix.
         * 
         * @param message the message to be printed
         */
        private void print(String message) {
			System.err.println(this.prefix + message);
		}

	}

}
