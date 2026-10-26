/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import java.io.Console;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ch.qos.logback.classic.LoggerContext;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link LogbackLoggingSystemProperties}.
 *
 * @author Phillip Webb
 * @author Van Vo
 */
class LogbackLoggingSystemPropertiesTests {

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
	void applySetsStandardSystemProperties() {
		this.environment.setProperty("logging.pattern.console", "boot");
		new LogbackLoggingSystemProperties(this.environment).apply();
		assertThat(System.getProperties())
			.containsEntry(LoggingSystemProperty.CONSOLE_PATTERN.getEnvironmentVariableName(), "boot");
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
			.containsEntry(RollingPolicySystemProperty.FILE_NAME_PATTERN.getEnvironmentVariableName(), "fnp")
			.containsEntry(RollingPolicySystemProperty.CLEAN_HISTORY_ON_START.getEnvironmentVariableName(), "chos")
			.containsEntry(RollingPolicySystemProperty.MAX_FILE_SIZE.getEnvironmentVariableName(), "1024")
			.containsEntry(RollingPolicySystemProperty.TOTAL_SIZE_CAP.getEnvironmentVariableName(), "2048")
			.containsEntry(RollingPolicySystemProperty.MAX_HISTORY.getEnvironmentVariableName(), "mh");
	}

	@Test
	void disabledTransferKeepsPropertiesLocalToEachLoggerContext() {
		Map<String, @Nullable String> previous = new HashMap<>();
		for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
			String name = property.getEnvironmentVariableName();
			previous.put(name, System.getProperty(name));
		}
		for (RollingPolicySystemProperty property : RollingPolicySystemProperty.values()) {
			String name = property.getEnvironmentVariableName();
			previous.put(name, System.getProperty(name));
		}
		previous.put("LOGGED_APPLICATION_NAME", System.getProperty("LOGGED_APPLICATION_NAME"));
		previous.put("org.jboss.logging.provider", System.getProperty("org.jboss.logging.provider"));
		LoggerContext first = new LoggerContext();
		LoggerContext second = new LoggerContext();
		try {
			this.environment.setProperty("logging.system-properties.enabled", "false");
			this.environment.setProperty("spring.application.name", "first");
			this.environment.setProperty("logging.file.name", "first.log");
			this.environment.setProperty("logging.logback.rollingpolicy.max-file-size", "1KB");
			new LogbackLoggingSystemProperties(this.environment, first::putProperty)
				.apply(LogFile.get(this.environment));
			this.environment.setProperty("spring.application.name", "second");
			this.environment.setProperty("logging.file.name", "second.log");
			this.environment.setProperty("logging.logback.rollingpolicy.max-file-size", "2KB");
			new LogbackLoggingSystemProperties(this.environment, second::putProperty)
				.apply(LogFile.get(this.environment));
			assertThat(first.getProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName()))
				.isEqualTo("first");
			assertThat(first.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
				.isEqualTo("first.log");
			assertThat(first.getProperty(RollingPolicySystemProperty.MAX_FILE_SIZE.getEnvironmentVariableName()))
				.isEqualTo("1024");
			assertThat(second.getProperty(LoggingSystemProperty.APPLICATION_NAME.getEnvironmentVariableName()))
				.isEqualTo("second");
			assertThat(second.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
				.isEqualTo("second.log");
			assertThat(second.getProperty(RollingPolicySystemProperty.MAX_FILE_SIZE.getEnvironmentVariableName()))
				.isEqualTo("2048");
			previous.forEach((name, value) -> assertThat(System.getProperty(name)).as(name).isEqualTo(value));
		}
		finally {
			first.stop();
			second.stop();
		}
	}

	@Test
	void consoleCharsetWhenNoPropertyUsesDefault() {
		LogbackLoggingSystemProperties logbackLoggingSystemProperties = spy(
				new LogbackLoggingSystemProperties(new MockEnvironment(), null, null));
		given(logbackLoggingSystemProperties.getConsole()).willReturn(null);

		logbackLoggingSystemProperties.apply(null);
		assertThat(System.getProperty(LoggingSystemProperty.CONSOLE_CHARSET.getEnvironmentVariableName()))
			.isEqualTo(Charset.defaultCharset().name());
	}

	@Test
	void consoleCharsetWhenNoPropertyUsesSystemConsoleCharsetWhenAvailable() {
		LogbackLoggingSystemProperties logbackLoggingSystemProperties = spy(
				new LogbackLoggingSystemProperties(new MockEnvironment(), null, null));

		Console console = mock(Console.class);
		given(console.charset()).willReturn(StandardCharsets.UTF_16BE);
		given(logbackLoggingSystemProperties.getConsole()).willReturn(console);

		logbackLoggingSystemProperties.apply(null);
		assertThat(System.getProperty(LoggingSystemProperty.CONSOLE_CHARSET.getEnvironmentVariableName()))
			.isEqualTo(StandardCharsets.UTF_16BE.name());
	}

	@Test
	void fileCharsetWhenNoPropertyUsesUtf8() {
		new LoggingSystemProperties(new MockEnvironment()).apply(null);
		assertThat(System.getProperty(LoggingSystemProperty.FILE_CHARSET.getEnvironmentVariableName()))
			.isEqualTo(StandardCharsets.UTF_8.name());
	}

}
