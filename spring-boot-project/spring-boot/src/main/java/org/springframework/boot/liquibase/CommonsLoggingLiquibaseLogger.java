/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.liquibase;

import liquibase.logging.LogLevel;
import liquibase.logging.LogType;
import liquibase.logging.Logger;
import liquibase.logging.core.AbstractLogger;
import org.apache.commons.logging.Log;

/**
 * Liquibase {@link Logger} that delegates to an Apache Commons {@link Log}.
 *
 * @author Michael Cramer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public class CommonsLoggingLiquibaseLogger extends AbstractLogger {

	private Log logger;

	public CommonsLoggingLiquibaseLogger(Log logger) {
		this.logger = logger;
	}

	@Override
	public void severe(String message) {
		if (isEnabled(LogLevel.SEVERE)) {
			this.logger.error(message);
		}
	}

	@Override
	public void severe(LogType logType, String message) {
		if (isEnabled(LogLevel.SEVERE)) {
			this.logger.error(message);
		}
	}

	@Override
	public void severe(LogType logType, String message, Throwable e) {
		if (isEnabled(LogLevel.SEVERE)) {
			this.logger.error((message), e);
		}
	}

	@Override
	public void severe(String message, Throwable e) {
		if (isEnabled(LogLevel.SEVERE)) {
			this.logger.error((message), e);
		}
	}

	@Override
	public void warning(String message) {
		if (isEnabled(LogLevel.WARNING)) {
			this.logger.warn(message);
		}
	}

	@Override
	public void warning(String message, Throwable e) {
		if (isEnabled(LogLevel.WARNING)) {
			this.logger.warn(message, e);
		}
	}

	@Override
	public void warning(LogType logType, String message) {
		if (isEnabled(LogLevel.WARNING)) {
			this.logger.warn(message);
		}
	}

	@Override
	public void warning(LogType logType, String message, Throwable e) {
		if (isEnabled(LogLevel.WARNING)) {
			this.logger.warn(message, e);
		}
	}

	@Override
	public void info(LogType logType, String message) {
		if (isEnabled(LogLevel.INFO)) {
			this.logger.info(message);
		}
	}

	@Override
	public void info(LogType logType, String message, Throwable e) {
		if (isEnabled(LogLevel.INFO)) {
			this.logger.info(message, e);
		}
	}

	@Override
	public void debug(LogType logType, String message) {
		if (isEnabled(LogLevel.DEBUG)) {
			this.logger.debug(message);
		}
	}

	@Override
	public void debug(LogType logType, String message, Throwable e) {
		if (isEnabled(LogLevel.DEBUG)) {
			this.logger.debug(message, e);
		}
	}

	private boolean isEnabled(LogLevel level) {
		if (this.logger != null) {
			switch (level) {
				case DEBUG:
					return this.logger.isDebugEnabled();
				case INFO:
					return this.logger.isInfoEnabled();
				case WARNING:
					return this.logger.isWarnEnabled();
				case SEVERE:
					return this.logger.isErrorEnabled();
			}
		}
		return false;
	}
}
