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

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4J2LoggingSystemProperties}.
 *
 * @author Andrey Timonin
 */
class Log4J2LoggingSystemPropertiesTests {

	private Set<Object> systemPropertyNames;

	private MockEnvironment environment;

	@BeforeEach
	void captureSystemPropertyNames() {
		for (LoggingSystemProperty loggingSystemProperties : LoggingSystemProperty.values()) {
			System.getProperties().remove(loggingSystemProperties);
		}
		this.systemPropertyNames = new HashSet<>(System.getProperties().keySet());
		this.environment = new MockEnvironment();
		this.environment
			.setConversionService((ConfigurableConversionService) ApplicationConversionService.getSharedInstance());
	}

	@AfterEach
	void restoreSystemProperties() {
		System.getProperties().keySet().retainAll(this.systemPropertyNames);
	}

	@Test
	void applySetsStandardSystemProperties() {
		this.environment.setProperty("logging.threshold.console", "lts");
		new Log4J2LoggingSystemProperties(this.environment).apply();
		assertThat(System.getProperties())
			.containsEntry(LoggingSystemProperty.CONSOLE_THRESHOLD.getEnvironmentVariableName(), "lts");
	}

	@Test
	void applySetsLog4J2SystemProperties() {
		this.environment.setProperty("logging.log4j2.rollingpolicy.file-name-pattern", "fnp");
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-file-size", "1KB");
		this.environment.setProperty("logging.log4j2.rollingpolicy.total-size-cap", "2KB");
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-history", "mh");
		new Log4J2LoggingSystemProperties(this.environment).apply();
		assertThat(System.getProperties())
			.containsEntry(Log4J2RollingPolicySystemProperty.FILE_NAME_PATTERN.getEnvironmentVariableName(), "fnp")
			.containsEntry(Log4J2RollingPolicySystemProperty.MAX_FILE_SIZE.getEnvironmentVariableName(), "1024")
			.containsEntry(Log4J2RollingPolicySystemProperty.TOTAL_SIZE_CAP.getEnvironmentVariableName(), "2048")
			.containsEntry(Log4J2RollingPolicySystemProperty.MAX_HISTORY.getEnvironmentVariableName(), "mh");
	}

}
