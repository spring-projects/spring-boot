/*
 * Copyright 2012-2020 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogbackLoggingSystemProperties}.
 *
 * @author Phillip Webb
 */
class LogbackLoggingSystemPropertiesTests {

	private Set<Object> systemPropertyNames;

	private MockEnvironment environment;

	@BeforeEach
	void captureSystemPropertyNames() {
		System.getProperties().remove(LoggingSystemProperties.CONSOLE_LOG_CHARSET);
		System.getProperties().remove(LoggingSystemProperties.FILE_LOG_CHARSET);
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
		this.environment.setProperty("logging.pattern.console", "boot");
		new LogbackLoggingSystemProperties(this.environment).apply();
		assertThat(System.getProperties()).containsEntry(LoggingSystemProperties.CONSOLE_LOG_PATTERN, "boot");
	}

	@Test
	void applySetsLogbackSystemProperties() {
		this.environment.setProperty("logging.logback.rollingpolicy.file-name-pattern", "fnp");
		this.environment.setProperty("logging.logback.rollingpolicy.clean-history-on-start", "chos");
		this.environment.setProperty("logging.logback.rollingpolicy.max-file-size", "1KB");
		this.environment.setProperty("logging.logback.rollingpolicy.total-size-cap", "2KB");
		this.environment.setProperty("logging.logback.rollingpolicy.max-history", "mh");
		new LogbackLoggingSystemProperties(this.environment).apply();
		assertThat(System.getProperties())
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_FILE_NAME_PATTERN, "fnp")
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_CLEAN_HISTORY_ON_START, "chos")
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_MAX_FILE_SIZE, "1024")
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_TOTAL_SIZE_CAP, "2048")
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_MAX_HISTORY, "mh");
	}

	@Test
	void applySetsLogbackSystemPropertiesFromDeprecated() {
		this.environment.setProperty("logging.pattern.rolling-file-name", "fnp");
		this.environment.setProperty("logging.file.clean-history-on-start", "chos");
		this.environment.setProperty("logging.file.max-size", "1KB");
		this.environment.setProperty("logging.file.total-size-cap", "2KB");
		this.environment.setProperty("logging.file.max-history", "mh");
		new LogbackLoggingSystemProperties(this.environment).apply();
		assertThat(System.getProperties())
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_FILE_NAME_PATTERN, "fnp")
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_CLEAN_HISTORY_ON_START, "chos")
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_MAX_FILE_SIZE, "1024")
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_TOTAL_SIZE_CAP, "2048")
				.containsEntry(LogbackLoggingSystemProperties.ROLLINGPOLICY_MAX_HISTORY, "mh");
	}

	@Test
	void consoleCharsetWhenNoPropertyUsesDefault() {
		new LoggingSystemProperties(new MockEnvironment()).apply(null);
		assertThat(System.getProperty(LoggingSystemProperties.CONSOLE_LOG_CHARSET))
				.isEqualTo(Charset.defaultCharset().name());
	}

	@Test
	void fileCharsetWhenNoPropertyUsesDefault() {
		new LoggingSystemProperties(new MockEnvironment()).apply(null);
		assertThat(System.getProperty(LoggingSystemProperties.FILE_LOG_CHARSET))
				.isEqualTo(Charset.defaultCharset().name());
	}

}
