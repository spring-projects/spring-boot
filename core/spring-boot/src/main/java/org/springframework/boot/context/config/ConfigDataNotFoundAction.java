/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.apache.commons.logging.Log;

import org.springframework.core.log.LogMessage;

/**
 * Action to take when an uncaught {@link ConfigDataNotFoundException} is thrown.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public enum ConfigDataNotFoundAction {

	/**
	 * Throw the exception to fail startup.
	 */
	FAIL {

		@Override
		void handle(Log logger, ConfigDataNotFoundException ex) {
			throw ex;
		}

	},

	/**
	 * Ignore the exception and continue processing the remaining locations.
	 */
	IGNORE {

		@Override
		void handle(Log logger, ConfigDataNotFoundException ex) {
			logger.trace(LogMessage.format("Ignoring missing config data %s", ex.getReferenceDescription()));
		}

	};

	/**
	 * Handle the given exception.
	 * @param logger the logger used for output {@code ConfigDataLocation})
	 * @param ex the exception to handle
	 */
	abstract void handle(Log logger, ConfigDataNotFoundException ex);

}
