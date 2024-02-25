/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.neo4j;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.Logger;
import org.neo4j.driver.Logging;

/**
 * Shim to use Spring JCL implementation, delegating all the hard work of deciding the
 * underlying system to Spring and Spring Boot.
 *
 * @author Michael J. Simons
 */
class Neo4jSpringJclLogging implements Logging {

	/**
	 * This prefix gets added to the log names the driver requests to add some namespace
	 * around it in a bigger application scenario.
	 */
	private static final String AUTOMATIC_PREFIX = "org.neo4j.driver.";

	/**
     * Returns a Logger instance for the specified name.
     * 
     * @param name the name of the logger
     * @return a Logger instance
     */
    @Override
	public Logger getLog(String name) {
		String requestedLog = name;
		if (!requestedLog.startsWith(AUTOMATIC_PREFIX)) {
			requestedLog = AUTOMATIC_PREFIX + name;
		}
		Log springJclLog = LogFactory.getLog(requestedLog);
		return new SpringJclLogger(springJclLog);
	}

	/**
     * SpringJclLogger class.
     */
    private static final class SpringJclLogger implements Logger {

		private final Log delegate;

		/**
         * Constructs a new SpringJclLogger with the specified Log delegate.
         * 
         * @param delegate the Log delegate to be used by the SpringJclLogger
         */
        SpringJclLogger(Log delegate) {
			this.delegate = delegate;
		}

		/**
         * Logs an error message with the specified message and cause.
         * 
         * @param message the error message to be logged
         * @param cause the cause of the error
         */
        @Override
		public void error(String message, Throwable cause) {
			this.delegate.error(message, cause);
		}

		/**
         * Logs an informational message with the specified format and parameters.
         * 
         * @param format the format string for the message
         * @param params the parameters to be inserted into the format string
         */
        @Override
		public void info(String format, Object... params) {
			this.delegate.info(String.format(format, params));
		}

		/**
         * Logs a warning message with the specified format and parameters.
         * 
         * @param format the format string for the warning message
         * @param params the parameters to be inserted into the format string
         */
        @Override
		public void warn(String format, Object... params) {
			this.delegate.warn(String.format(format, params));
		}

		/**
         * Logs a warning message with the specified message and cause.
         * 
         * @param message the warning message to be logged
         * @param cause the cause of the warning
         */
        @Override
		public void warn(String message, Throwable cause) {
			this.delegate.warn(message, cause);
		}

		/**
         * Logs a debug message with the specified format and parameters.
         * 
         * @param format the format string for the debug message
         * @param params the parameters to be inserted into the format string
         */
        @Override
		public void debug(String format, Object... params) {
			if (isDebugEnabled()) {
				this.delegate.debug(String.format(format, params));
			}
		}

		/**
         * Logs a debug message with an associated throwable.
         * 
         * @param message   the debug message to be logged
         * @param throwable the throwable associated with the debug message
         */
        @Override
		public void debug(String message, Throwable throwable) {
			if (isDebugEnabled()) {
				this.delegate.debug(message, throwable);
			}
		}

		/**
         * Logs a trace message with the specified format and parameters.
         * 
         * @param format the format string for the trace message
         * @param params the parameters to be inserted into the format string
         */
        @Override
		public void trace(String format, Object... params) {
			if (isTraceEnabled()) {
				this.delegate.trace(String.format(format, params));
			}
		}

		/**
         * Returns a boolean value indicating whether trace level logging is enabled for this logger.
         * 
         * @return {@code true} if trace level logging is enabled, {@code false} otherwise
         */
        @Override
		public boolean isTraceEnabled() {
			return this.delegate.isTraceEnabled();
		}

		/**
         * Returns a boolean value indicating whether debug logging is enabled.
         *
         * @return {@code true} if debug logging is enabled, {@code false} otherwise.
         */
        @Override
		public boolean isDebugEnabled() {
			return this.delegate.isDebugEnabled();
		}

	}

}
