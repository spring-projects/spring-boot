/*
 * Copyright 2012-present the original author or authors.
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
	void log4j2CanBeForcedUsingSystemProperty() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, Log4J2LoggingSystem.class.getName());
		assertThat(LoggingSystem.get(getClass().getClassLoader())).isInstanceOf(Log4J2LoggingSystem.class);
	}

	@Test
	void julCanBeForcedUsingSystemProperty() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, JavaLoggingSystem.class.getName());
		assertThat(LoggingSystem.get(getClass().getClassLoader())).isInstanceOf(JavaLoggingSystem.class);
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
