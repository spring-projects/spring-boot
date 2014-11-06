package org.springframework.boot.liquibase;

import liquibase.configuration.LiquibaseConfiguration;
import liquibase.logging.LogLevel;
import liquibase.logging.Logger;
import liquibase.logging.core.AbstractLogger;
import liquibase.logging.core.DefaultLoggerConfiguration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Liqubase {@link Logger} that that delegates to an Apache Commons {@link Log}.
 *
 * @author Michael Cramer
 * @author Phillip Webb
 * @since 1.2.0
 */
public class CommonsLoggingLiquibaseLogger extends AbstractLogger {

	public static final int PRIORITY = 10;

	private Log logger;

	@Override
	public void setName(String name) {
		this.logger = createLogger(name);
	}

	/**
	 * Factory method used to create the logger.
	 * @param name the name of the logger
	 * @return a {@link Log} instance
	 */
	protected Log createLogger(String name) {
		return LogFactory.getLog(name);
	}

	@Override
	public void setLogLevel(String logLevel, String logFile) {
		super.setLogLevel(logLevel);
	}

	@Override
	public void severe(String message) {
		if (isEnabled(LogLevel.SEVERE)) {
			this.logger.error(buildMessage(message));
		}
	}

	@Override
	public void severe(String message, Throwable e) {
		if (isEnabled(LogLevel.SEVERE)) {
			this.logger.error(buildMessage(message), e);
		}
	}

	@Override
	public void warning(String message) {
		if (isEnabled(LogLevel.WARNING)) {
			this.logger.warn(buildMessage(message));
		}
	}

	@Override
	public void warning(String message, Throwable e) {
		if (isEnabled(LogLevel.WARNING)) {
			this.logger.warn(buildMessage(message), e);
		}
	}

	@Override
	public void info(String message) {
		if (isEnabled(LogLevel.INFO)) {
			this.logger.info(buildMessage(message));
		}
	}

	@Override
	public void info(String message, Throwable e) {
		if (isEnabled(LogLevel.INFO)) {
			this.logger.info(buildMessage(message), e);
		}
	}

	@Override
	public void debug(String message) {
		if (isEnabled(LogLevel.DEBUG)) {
			this.logger.debug(buildMessage(message));
		}
	}

	@Override
	public void debug(String message, Throwable e) {
		if (isEnabled(LogLevel.DEBUG)) {
			this.logger.debug(buildMessage(message), e);
		}
	}

	@Override
	public int getPriority() {
		return PRIORITY;
	}

	private boolean isEnabled(LogLevel level) {
		if (this.logger != null && getLogLevel().compareTo(level) <= 0) {
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

	@Override
	public LogLevel getLogLevel() {
		LogLevel logLevel = super.getLogLevel();
		if (logLevel == null) {
			return toLogLevel(LiquibaseConfiguration.getInstance()
					.getConfiguration(DefaultLoggerConfiguration.class).getLogLevel());
		}
		return logLevel;
	}

}
