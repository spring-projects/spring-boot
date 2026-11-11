/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import org.apache.commons.logging.Log;
import org.jspecify.annotations.Nullable;

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

	private final @Nullable LogMethod logMethod;

	LogLevel(@Nullable LogMethod logMethod) {
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
	public void log(@Nullable Log logger, Object message, @Nullable Throwable cause) {
		if (logger != null && this.logMethod != null) {
			this.logMethod.log(logger, message, cause);
		}
	}

	@FunctionalInterface
	private interface LogMethod {

		void log(Log logger, Object message, @Nullable Throwable cause);

	}

}
