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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code log4j2.xml}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Vasily Pelikh
 */
class Log4j2XmlTests {

	protected @Nullable Configuration configuration;

	private LoggerContext loggerContext;

	@BeforeEach
	void setup() {
		MockEnvironment environment = new MockEnvironment();
		this.loggerContext = new LoggerContext("test");
		this.loggerContext.putObject(Log4J2LoggingSystem.ENVIRONMENT_KEY, environment);
	}

	@AfterEach
	void cleanup() {
		this.loggerContext.removeObject(Log4J2LoggingSystem.ENVIRONMENT_KEY);
	}

	@AfterEach
	void stopConfiguration() {
		if (this.configuration != null) {
			this.configuration.stop();
		}
	}

	@Test
	void whenLogExceptionConversionWordIsNotConfiguredThenConsoleUsesDefault() {
		assertThat(consoleAppenderLayout()).asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
			.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
			.contains("%xwEx");
	}

	@Test
	void whenLogExceptionConversionWordIsSetThenConsoleUsesIt() {
		withSystemProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.getEnvironmentVariableName(), "custom",
				() -> assertThat(consoleAppenderLayout())
					.asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
					.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
					.contains("custom"));
	}

	@Test
	void whenLogLevelPatternIsNotConfiguredThenConsoleUsesDefault() {
		assertThat(consoleAppenderLayout()).asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
			.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
			.contains("%5p");
	}

	@Test
	void whenLogLevelPatternIsSetThenConsoleUsesIt() {
		withSystemProperty(LoggingSystemProperty.LEVEL_PATTERN.getEnvironmentVariableName(), "custom",
				() -> assertThat(consoleAppenderLayout())
					.asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
					.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
					.contains("custom"));
	}

	@Test
	void whenLogLDateformatPatternIsNotConfiguredThenConsoleUsesDefault() {
		assertThat(consoleAppenderLayout()).asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
			.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
			.contains("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	}

	@Test
	void whenLogDateformatPatternIsSetThenConsoleUsesIt() {
		withSystemProperty(LoggingSystemProperty.DATEFORMAT_PATTERN.getEnvironmentVariableName(), "dd-MM-yyyy",
				() -> assertThat(consoleAppenderLayout())
					.asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
					.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
					.contains("dd-MM-yyyy"));
	}

	@Test
	void whenStructuredLogIsDisabledThenConsoleUsesDefault() {
		Map<String, String> properties = Map
			.of(LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "true");
		withSystemProperties(properties, () -> assertThat(consoleAppenderLayout()).isInstanceOf(PatternLayout.class));
	}

	@Test
	void whenStructuredLogIsEnabledAndFormatIsNotSetThenConsoleUsesDefault() {
		Map<String, String> properties = Map
			.of(LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false");
		withSystemProperties(properties, () -> assertThat(consoleAppenderLayout()).isInstanceOf(PatternLayout.class));
	}

	@Test
	void whenStructuredLogIsEnabledAndFormatIsEmptyThenConsoleUsesDefault() {
		Map<String, String> properties = Map.of(
				LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false",
				LoggingSystemProperty.CONSOLE_STRUCTURED_FORMAT.getEnvironmentVariableName(), "");
		withSystemProperties(properties, () -> assertThat(consoleAppenderLayout()).isInstanceOf(PatternLayout.class));
	}

	@Test
	void whenStructuredLogIsEnabledThenConsoleUsesIt() {
		Map<String, String> properties = Map.of(
				LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false",
				LoggingSystemProperty.CONSOLE_STRUCTURED_FORMAT.getEnvironmentVariableName(), "ecs");
		withSystemProperties(properties,
				() -> assertThat(consoleAppenderLayout()).isInstanceOf(StructuredLogLayout.class));
	}

	@Test
	void whenStructuredLogIsEnabledAndCharsetIsSetThenConsoleUsesIt() {
		Map<String, String> properties = Map.of(
				LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false",
				LoggingSystemProperty.CONSOLE_STRUCTURED_FORMAT.getEnvironmentVariableName(), "ecs",
				LoggingSystemProperty.CONSOLE_CHARSET.getEnvironmentVariableName(), "ISO_8859_1");
		withSystemProperties(properties,
				() -> assertThat(consoleAppenderLayout())
					.asInstanceOf(InstanceOfAssertFactories.type(StructuredLogLayout.class))
					.extracting(StructuredLogLayout::getCharset, InstanceOfAssertFactories.type(Charset.class))
					.isEqualTo(StandardCharsets.ISO_8859_1));
	}

	@Test
	void whenStructuredLogIsEnabledAndCharsetIsNotSetThenConsoleUsesStructuredLoggingWithDefaultCharset() {
		Map<String, String> properties = Map.of(
				LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false",
				LoggingSystemProperty.CONSOLE_STRUCTURED_FORMAT.getEnvironmentVariableName(), "ecs");
		withSystemProperties(properties,
				() -> assertThat(consoleAppenderLayout())
					.asInstanceOf(InstanceOfAssertFactories.type(StructuredLogLayout.class))
					.extracting(StructuredLogLayout::getCharset, InstanceOfAssertFactories.type(Charset.class))
					.isEqualTo(StandardCharsets.UTF_8));
	}

	protected void withSystemProperty(String name, String value, Runnable action) {
		String previous = System.setProperty(name, value);
		action.run();
		if (previous == null) {
			System.clearProperty(name);
		}
		else {
			System.setProperty(name, previous);
		}
	}

	protected static void withSystemProperties(Map<String, String> properties, Runnable action) {
		Map<String, String> previousMap = new HashMap<>(properties.size());
		properties.forEach((name, newValue) -> {
			String previousValue = System.getProperty(name);
			previousMap.put(name, previousValue);

			System.setProperty(name, newValue);
		});

		try {
			action.run();
		}
		finally {
			properties.forEach((name, newValue) -> {
				String previousValue = previousMap.get(name);
				if (previousValue != null) {
					System.setProperty(name, previousValue);
				}
				else {
					System.clearProperty(name);
				}
			});
		}
	}

	private Layout<? extends Serializable> consoleAppenderLayout() {
		prepareConfiguration();
		assertThat(this.configuration).isNotNull();
		return this.configuration.getAppender("Console").getLayout();
	}

	protected void prepareConfiguration() {
		this.configuration = initializeConfiguration();
		this.configuration.start();
	}

	protected Configuration initializeConfiguration() {
		Configuration configuration = ConfigurationFactory.getInstance()
			.getConfiguration(this.loggerContext, configurationSource());
		configuration.initialize();
		return configuration;
	}

	private ConfigurationSource configurationSource() {
		try (InputStream in = getClass().getResourceAsStream(getConfigFileName())) {
			return new ConfigurationSource(in);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected String getConfigFileName() {
		return "log4j2.xml";
	}

}
