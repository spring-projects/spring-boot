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

import org.jspecify.annotations.Nullable;

/**
 * Log4j2 rolling policy system properties that can later be used by log configuration
 * files.
 *
 * @author HoJoo Moon
 * @since 4.0.0
 * @see Log4j2LoggingSystemProperties
 */
public enum Log4j2RollingPolicySystemProperty {

	/**
	 * Logging system property for the rolled-over log file name pattern.
	 */
	FILE_NAME_PATTERN("file-name-pattern", "logging.pattern.rolling-file-name"),

	/**
	 * Logging system property for the file log max size.
	 */
	MAX_FILE_SIZE("max-file-size", "logging.file.max-size"),

	/**
	 * Logging system property for the file log max history.
	 */
	MAX_HISTORY("max-history", "logging.file.max-history"),

	/**
	 * Logging system property for the rolling policy strategy.
	 */
	STRATEGY("strategy", null),

	/**
	 * Logging system property for the rolling policy time interval.
	 */
	TIME_INTERVAL("time-based.interval", null),

	/**
	 * Logging system property for the rolling policy time modulate flag.
	 */
	TIME_MODULATE("time-based.modulate", null),

	/**
	 * Logging system property for the cron based schedule.
	 */
	CRON_SCHEDULE("cron.schedule", null);

	private final String environmentVariableName;

	private final String applicationPropertyName;

	private final @Nullable String deprecatedApplicationPropertyName;

	Log4j2RollingPolicySystemProperty(String applicationPropertyName,
			@Nullable String deprecatedApplicationPropertyName) {
		this.environmentVariableName = "LOG4J2_ROLLINGPOLICY_" + name();
		this.applicationPropertyName = "logging.log4j2.rollingpolicy." + applicationPropertyName;
		this.deprecatedApplicationPropertyName = deprecatedApplicationPropertyName;
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

	@Nullable String getDeprecatedApplicationPropertyName() {
		return this.deprecatedApplicationPropertyName;
	}

}
