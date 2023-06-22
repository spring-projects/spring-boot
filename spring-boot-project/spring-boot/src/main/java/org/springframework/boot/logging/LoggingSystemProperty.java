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

/**
 * Logging system properties that can later be used by log configuration files.
 *
 * @author Phillip Webb
 * @since 3.2.0
 * @see LoggingSystemProperties
 */
public enum LoggingSystemProperty {

	/**
	 * Logging system property for the application name that should be logged.
	 */
	APPLICATION_NAME("LOGGED_APPLICATION_NAME"),

	/**
	 * Logging system property for the process ID.
	 */
	PID("PID"),

	/**
	 * Logging system property for the log file.
	 */
	LOG_FILE("LOG_FILE"),

	/**
	 * Logging system property for the log path.
	 */
	LOG_PATH("LOG_PATH"),

	/**
	 * Logging system property for the console log charset.
	 */
	CONSOLE_CHARSET("CONSOLE_LOG_CHARSET", "logging.charset.console"),

	/**
	 * Logging system property for the file log charset.
	 */
	FILE_CHARSET("FILE_LOG_CHARSET", "logging.charset.file"),

	/**
	 * Logging system property for the console log.
	 */
	CONSOLE_THRESHOLD("CONSOLE_LOG_THRESHOLD", "logging.threshold.console"),

	/**
	 * Logging system property for the file log.
	 */
	FILE_THRESHOLD("FILE_LOG_THRESHOLD", "logging.threshold.file"),

	/**
	 * Logging system property for the exception conversion word.
	 */
	EXCEPTION_CONVERSION_WORD("LOG_EXCEPTION_CONVERSION_WORD", "logging.exception-conversion-word"),

	/**
	 * Logging system property for the console log pattern.
	 */
	CONSOLE_PATTERN("CONSOLE_LOG_PATTERN", "logging.pattern.console"),

	/**
	 * Logging system property for the file log pattern.
	 */
	FILE_PATTERN("FILE_LOG_PATTERN", "logging.pattern.file"),

	/**
	 * Logging system property for the log level pattern.
	 */
	LEVEL_PATTERN("LOG_LEVEL_PATTERN", "logging.pattern.level"),

	/**
	 * Logging system property for the date-format pattern.
	 */
	DATEFORMAT_PATTERN("LOG_DATEFORMAT_PATTERN", "logging.pattern.dateformat"),

	/**
	 * Logging system property for the correlation pattern.
	 */
	CORRELATION_PATTERN("LOG_CORRELATION_PATTERN", "logging.pattern.correlation");

	private final String environmentVariableName;

	private final String applicationPropertyName;

	LoggingSystemProperty(String environmentVariableName) {
		this(environmentVariableName, null);
	}

	LoggingSystemProperty(String environmentVariableName, String applicationPropertyName) {
		this.environmentVariableName = environmentVariableName;
		this.applicationPropertyName = applicationPropertyName;
	}

	/**
	 * Return the name of environment variable that can be used to access this property.
	 * @return the environment variable name
	 */
	public String getEnvironmentVariableName() {
		return this.environmentVariableName;
	}

	String getApplicationPropertyName() {
		return this.applicationPropertyName;
	}

}
