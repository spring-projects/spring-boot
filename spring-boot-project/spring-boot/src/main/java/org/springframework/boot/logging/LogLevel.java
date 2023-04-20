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

package org.springframework.boot.logging;

import org.apache.commons.logging.Log;

/**
 * Logging levels supported by a {@link LoggingSystem}.
 *
 * @author Phillip Webb
 * @since 1.0.0
 */
public enum LogLevel {

	TRACE(Log::trace),

	DEBUG(Log::debug),

	INFO(Log::info),

	WARN(Log::warn),

	ERROR(Log::error),

	FATAL(Log::fatal),

	OFF(null);

	private final LogMethod logMethod;

	LogLevel(LogMethod logMethod) {
		this.logMethod = logMethod;
	}

	/**
	 * Log a message to the given logger at this level.
	 * @param logger the logger
	 * @param message the message to log
	 * @since 3.1.0
	 */
	public void log(Log logger, Object message) {
		log(logger, message, null);
	}

	/**
	 * Log a message to the given logger at this level.
	 * @param logger the logger
	 * @param message the message to log
	 * @param cause the cause to log
	 * @since 3.1.0
	 */
	public void log(Log logger, Object message, Throwable cause) {
		if (logger != null && this.logMethod != null) {
			this.logMethod.log(logger, message, cause);
		}
	}

	@FunctionalInterface
	private interface LogMethod {

		void log(Log logger, Object message, Throwable cause);

	}

}
