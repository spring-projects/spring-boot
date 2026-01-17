
package org.springframework.boot.logging.log4j2;

/**
 * Log4j2 rolling policy system properties that can later be used by log configuration
 * files.
 *
 * @see Log4J2LoggingSystemProperties
 * @author Andrey Timonin
 * @since 4.1.0
 */
public enum Log4J2RollingPolicySystemProperty {

	/**
	 * Logging system property for the rolled-over log file name pattern.
	 */
	FILE_NAME_PATTERN("file-name-pattern"),

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

	Log4J2RollingPolicySystemProperty(String applicationPropertyName) {
		this.environmentVariableName = "LOG4J2_ROLLINGPOLICY_" + name();
		this.applicationPropertyName = "logging.log4j2.rollingpolicy." + applicationPropertyName;
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
