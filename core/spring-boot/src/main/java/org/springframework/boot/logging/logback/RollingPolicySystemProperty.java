/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

/**
 * Logback rolling policy system properties that can later be used by log configuration
 * files.
 *
 * @author Phillip Webb
 * @since 3.2.0
 * @see LogbackLoggingSystemProperties
 */
public enum RollingPolicySystemProperty {

	/**
	 * Logging system property for the rolled-over log file name pattern.
	 */
	FILE_NAME_PATTERN("file-name-pattern"),

	/**
	 * Logging system property for the clean history on start flag.
	 */
	CLEAN_HISTORY_ON_START("clean-history-on-start"),

	/**
	 * Logging system property for the file log max size.
	 */
	MAX_FILE_SIZE("max-file-size"),

	/**
	 * Logging system property for the file total size cap.
	 */
	TOTAL_SIZE_CAP("total-size-cap"),

	/**
	 * Logging system property for the file log max history.
	 */
	MAX_HISTORY("max-history");

	private final String environmentVariableName;

	private final String applicationPropertyName;

	RollingPolicySystemProperty(String applicationPropertyName) {
		this.environmentVariableName = "LOGBACK_ROLLINGPOLICY_" + name();
		this.applicationPropertyName = "logging.logback.rollingpolicy." + applicationPropertyName;
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
