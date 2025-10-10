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

package org.springframework.boot.logging;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.LoggingSystem.NoOpLoggingSystem;
import org.springframework.boot.logging.java.JavaLoggingSystem;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.boot.logging.logback.LogbackLoggingSystem;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link LoggingSystem}.
 *
 * @author Andy Wilkinson
 */
class LoggingSystemTests {

	@AfterEach
	void clearSystemProperty() {
		System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
	}

	@Test
	void logbackIsTheDefaultLoggingSystem() {
		assertThat(LoggingSystem.get(getClass().getClassLoader())).isInstanceOf(LogbackLoggingSystem.class);
	}

	@Test
	@ClassPathExclusions("logback-*.jar")
	void log4j2IsUsedInTheAbsenceOfLogback() {
		assertThat(LoggingSystem.get(getClass().getClassLoader())).isInstanceOf(Log4J2LoggingSystem.class);
	}

	@Test
	@ClassPathExclusions({ "logback-*.jar", "log4j-*.jar" })
	void julIsUsedInTheAbsenceOfLogbackAndLog4j2() {
		assertThat(LoggingSystem.get(getClass().getClassLoader())).isInstanceOf(JavaLoggingSystem.class);
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

	private static final class StubLoggingSystem extends LoggingSystem {

		@Override
		public void beforeInitialize() {
			// Stub implementation
		}

		@Override
		public void setLogLevel(@Nullable String loggerName, @Nullable LogLevel level) {
			// Stub implementation
		}

	}

}
