/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging.log4j2;

/**
 * Log4j2 rolling policy system properties that can later be used by log configuration
 * files.
 *
 * @author HoJoo Moon
 * @author Stephane Nicoll
 * @since 4.1.0
 * @see Log4j2LoggingSystemProperties
 */
public enum RollingPolicySystemProperty {

	/**
	 * Logging system property for the rolled-over log file name pattern.
	 */
	FILE_NAME_PATTERN("file-name-pattern"),

	/**
	 * Logging system property for the file log max size.
	 */
	MAX_FILE_SIZE("max-file-size"),

	/**
	 * Logging system property for the file log max history.
	 */
	MAX_HISTORY("max-history"),

	/**
	 * Logging system property for the {@linkplain RollingPolicyStrategy rolling policy
	 * strategy}.
	 */
	STRATEGY("strategy"),

	/**
	 * Logging system property for the rolling policy time interval.
	 */
	TIME_INTERVAL("time-interval"),

	/**
	 * Logging system property for the rolling policy time modulate flag.
	 */
	TIME_MODULATE("time-modulate"),

	/**
	 * Logging system property for the cron based schedule.
	 */
	CRON("cron");

	private final String environmentVariableName;

	private final String applicationPropertyName;

	RollingPolicySystemProperty(String applicationPropertyName) {
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
