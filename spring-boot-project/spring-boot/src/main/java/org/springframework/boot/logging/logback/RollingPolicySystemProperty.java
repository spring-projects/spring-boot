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
	FILE_NAME_PATTERN("file-name-pattern", "logging.pattern.rolling-file-name"),

	/**
	 * Logging system property for the clean history on start flag.
	 */
	CLEAN_HISTORY_ON_START("clean-history-on-start", "logging.file.clean-history-on-start"),

	/**
	 * Logging system property for the file log max size.
	 */
	MAX_FILE_SIZE("max-file-size", "logging.file.max-size"),

	/**
	 * Logging system property for the file total size cap.
	 */
	TOTAL_SIZE_CAP("total-size-cap", "logging.file.total-size-cap"),

	/**
	 * Logging system property for the file log max history.
	 */
	MAX_HISTORY("max-history", "logging.file.max-history");

	private final String environmentVariableName;

	private final String applicationPropertyName;

	private final String deprecatedApplicationPropertyName;

	RollingPolicySystemProperty(String applicationPropertyName, String deprecatedApplicationPropertyName) {
		this.environmentVariableName = "LOGBACK_ROLLINGPOLICY_" + name();
		this.applicationPropertyName = "logging.logback.rollingpolicy." + applicationPropertyName;
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

	String getDeprecatedApplicationPropertyName() {
		return this.deprecatedApplicationPropertyName;
	}

}
