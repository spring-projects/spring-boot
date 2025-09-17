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
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4j2LoggingSystemProperties}.
 *
 * @author hojooo
 */
class Log4j2LoggingSystemPropertiesTests {

	private Set<Object> systemPropertyNames;

	private MockEnvironment environment;

	@BeforeEach
	void captureSystemPropertyNames() {
		for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
			System.getProperties().remove(property.getEnvironmentVariableName());
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
	void appliesLog4j2RollingPolicyProperties() {
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-file-size", "50MB");
		this.environment.setProperty("logging.log4j2.rollingpolicy.clean-history-on-start", "true");
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-history", "30");
		this.environment.setProperty("logging.log4j2.rollingpolicy.total-size-cap", "10GB");
		this.environment.setProperty("logging.log4j2.rollingpolicy.file-name-pattern", "test.%d{yyyy-MM-dd}.%i.log");
		this.environment.setProperty("logging.log4j2.rollingpolicy.strategy", "time");
		this.environment.setProperty("logging.log4j2.rollingpolicy.time-based.interval", "2");
		this.environment.setProperty("logging.log4j2.rollingpolicy.time-based.modulate", "true");
		this.environment.setProperty("logging.log4j2.rollingpolicy.cron.schedule", "0 0 0 * * ?");

		new Log4j2LoggingSystemProperties(this.environment).apply(null);

		assertThat(System.getProperties())
			.containsEntry("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE", String.valueOf(DataSize.ofMegabytes(50).toBytes()))
			.containsEntry("LOG4J2_ROLLINGPOLICY_CLEAN_HISTORY_ON_START", "true")
			.containsEntry("LOG4J2_ROLLINGPOLICY_MAX_HISTORY", "30")
			.containsEntry("LOG4J2_ROLLINGPOLICY_TOTAL_SIZE_CAP", String.valueOf(DataSize.ofGigabytes(10).toBytes()))
			.containsEntry("LOG4J2_ROLLINGPOLICY_FILE_NAME_PATTERN", "test.%d{yyyy-MM-dd}.%i.log")
			.containsEntry("LOG4J2_ROLLINGPOLICY_STRATEGY", "time")
			.containsEntry("LOG4J2_ROLLINGPOLICY_TIME_INTERVAL", "2")
			.containsEntry("LOG4J2_ROLLINGPOLICY_TIME_MODULATE", "true")
			.containsEntry("LOG4J2_ROLLINGPOLICY_CRON_SCHEDULE", "0 0 0 * * ?");
	}

	@Test
	void appliesLog4j2RollingPolicyPropertiesWithDefaults() {
		new Log4j2LoggingSystemProperties(this.environment).apply(null);
		assertThat(System.getProperties().keySet())
			.noneMatch((key) -> ((String) key).startsWith("LOG4J2_ROLLINGPOLICY"));
	}

	@Test
	void appliesDeprecatedProperties() {
		this.environment.setProperty("logging.file.max-size", "20MB");
		this.environment.setProperty("logging.file.max-history", "15");
		new Log4j2LoggingSystemProperties(this.environment).apply(null);
		assertThat(System.getProperties())
			.containsEntry("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE", String.valueOf(DataSize.ofMegabytes(20).toBytes()))
			.containsEntry("LOG4J2_ROLLINGPOLICY_MAX_HISTORY", "15");
	}

	@Test
	void newPropertiesOverrideDeprecatedProperties() {
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-file-size", "100MB");
		this.environment.setProperty("logging.file.max-size", "20MB");
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-history", "50");
		this.environment.setProperty("logging.file.max-history", "15");
		new Log4j2LoggingSystemProperties(this.environment).apply(null);
		assertThat(System.getProperties())
			.containsEntry("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE", String.valueOf(DataSize.ofMegabytes(100).toBytes()))
			.containsEntry("LOG4J2_ROLLINGPOLICY_MAX_HISTORY", "50");
	}

	@Test
	void appliesWithLogFile() {
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-file-size", "25MB");
		LogFile logFile = LogFile.get(this.environment);
		new Log4j2LoggingSystemProperties(this.environment).apply(logFile);
		assertThat(System.getProperties())
			.containsEntry("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE", String.valueOf(DataSize.ofMegabytes(25).toBytes()));
	}

}
