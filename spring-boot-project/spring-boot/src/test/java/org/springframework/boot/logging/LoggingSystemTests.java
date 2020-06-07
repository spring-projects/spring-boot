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

package org.springframework.boot.logging;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.LoggingSystem.NoOpLoggingSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link LoggingSystem}.
 *
 * @author Andy Wilkinson
 * @author Vladislav Kisel
 */
class LoggingSystemTests {

	@AfterEach
	void clearSystemProperty() {
		System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
	}

	@Test
	void loggingSystemCanBeDisabled() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE);
		LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
		assertThat(loggingSystem).isInstanceOf(NoOpLoggingSystem.class);
	}

	@Test
	void getLoggerConfigurationIsUnsupported() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> new StubLoggingSystem().getLoggerConfiguration("test-logger-name"));
	}

	@Test
	void listLoggerConfigurationsIsUnsupported() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> new StubLoggingSystem().getLoggerConfigurations());
	}

	@Test
	void setLoggerLevelDelayed() throws InterruptedException {
		StubLoggingSystem loggingSystem = new StubLoggingSystem();

		loggingSystem.setLogLevel("ROOT", LogLevel.INFO);
		loggingSystem.setLogLevelDelayed("ROOT", LogLevel.DEBUG, Duration.ofMillis(20));

		assertThat(loggingSystem.loggerNameToLevel.get("ROOT")).isEqualTo(LogLevel.INFO);
		Thread.sleep(30);
		assertThat(loggingSystem.loggerNameToLevel.get("ROOT")).isEqualTo(LogLevel.DEBUG);
	}

	private static final class StubLoggingSystem extends LoggingSystem {

		Map<String, LogLevel> loggerNameToLevel = new HashMap<>();

		@Override
		public void beforeInitialize() {
			// Stub implementation
		}

		@Override
		public void setLogLevel(String loggerName, LogLevel level) {
			this.loggerNameToLevel.put(loggerName, level);
		}

	}

}
