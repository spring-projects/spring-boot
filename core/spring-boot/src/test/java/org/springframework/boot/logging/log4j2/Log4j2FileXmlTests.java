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

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.logging.LoggingSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@code log4j2-file.xml}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Vasily Pelikh
 */
class Log4j2FileXmlTests extends Log4j2XmlTests {

	@TempDir
	@SuppressWarnings("NullAway.Init")
	File temp;

	@Override
	@AfterEach
	void stopConfiguration() {
		super.stopConfiguration();
		for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
			System.getProperties().remove(property.getEnvironmentVariableName());
		}
	}

	@Test
	void whenLogExceptionConversionWordIsNotConfiguredThenFileAppenderUsesDefault() {
		assertThat(fileAppenderLayout()).asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
			.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
			.contains("%xwEx");
	}

	@Test
	void whenLogExceptionConversionWordIsSetThenFileAppenderUsesIt() {
		withSystemProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.getEnvironmentVariableName(), "custom",
				() -> assertThat(fileAppenderLayout()).asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
					.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
					.contains("custom"));
	}

	@Test
	void whenLogLevelPatternIsNotConfiguredThenFileAppenderUsesDefault() {
		assertThat(fileAppenderLayout()).asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
			.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
			.contains("%5p");
	}

	@Test
	void whenLogLevelPatternIsSetThenFileAppenderUsesIt() {
		withSystemProperty(LoggingSystemProperty.LEVEL_PATTERN.getEnvironmentVariableName(), "custom",
				() -> assertThat(fileAppenderLayout()).asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
					.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
					.contains("custom"));
	}

	@Test
	void whenLogLDateformatPatternIsNotConfiguredThenFileAppenderUsesDefault() {
		assertThat(fileAppenderLayout()).asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
			.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
			.contains("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	}

	@Test
	void whenLogDateformatPatternIsSetThenFileAppenderUsesIt() {
		withSystemProperty(LoggingSystemProperty.DATEFORMAT_PATTERN.getEnvironmentVariableName(), "dd-MM-yyyy",
				() -> assertThat(fileAppenderLayout()).asInstanceOf(InstanceOfAssertFactories.type(PatternLayout.class))
					.extracting(PatternLayout::getConversionPattern, InstanceOfAssertFactories.STRING)
					.contains("dd-MM-yyyy"));
	}

	@Test
	void whenStructuredLogIsDisabledThenFileAppenderUsesDefault() {
		Map<String, String> properties = Map
			.of(LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "true");
		withSystemProperties(properties, () -> assertThat(fileAppenderLayout()).isInstanceOf(PatternLayout.class));
	}

	@Test
	void whenStructuredLogIsEnabledAndFormatIsNotSetThenFileAppenderUsesDefault() {
		Map<String, String> properties = Map
			.of(LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false");
		withSystemProperties(properties, () -> assertThat(fileAppenderLayout()).isInstanceOf(PatternLayout.class));
	}

	@Test
	void whenStructuredLogIsEnabledAndFormatIsEmptyThenFileAppenderUsesDefault() {
		Map<String, String> properties = Map.of(
				LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false",
				LoggingSystemProperty.FILE_STRUCTURED_FORMAT.getEnvironmentVariableName(), "");
		withSystemProperties(properties, () -> assertThat(fileAppenderLayout()).isInstanceOf(PatternLayout.class));
	}

	@Test
	void whenStructuredLogIsEnabledThenFileAppenderUsesIt() {
		Map<String, String> properties = Map.of(
				LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false",
				LoggingSystemProperty.FILE_STRUCTURED_FORMAT.getEnvironmentVariableName(), "ecs");
		withSystemProperties(properties,
				() -> assertThat(fileAppenderLayout()).isInstanceOf(StructuredLogLayout.class));
	}

	@Test
	void whenStructuredLogIsEnabledAndCharsetIsSetThenFileAppenderUsesIt() {
		Map<String, String> properties = Map.of(
				LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false",
				LoggingSystemProperty.FILE_STRUCTURED_FORMAT.getEnvironmentVariableName(), "ecs",
				LoggingSystemProperty.FILE_CHARSET.getEnvironmentVariableName(), "ISO_8859_1");
		withSystemProperties(properties,
				() -> assertThat(fileAppenderLayout())
					.asInstanceOf(InstanceOfAssertFactories.type(StructuredLogLayout.class))
					.extracting(StructuredLogLayout::getCharset, InstanceOfAssertFactories.type(Charset.class))
					.isEqualTo(StandardCharsets.ISO_8859_1));
	}

	@Test
	void whenStructuredLogIsEnabledAndCharsetIsNotSetThenFileAppenderUsesStructuredLoggingWithDefaultCharset() {
		Map<String, String> properties = Map.of(
				LoggingSystemProperty.STRUCTURED_LOGGING_DISABLED.getEnvironmentVariableName(), "false",
				LoggingSystemProperty.FILE_STRUCTURED_FORMAT.getEnvironmentVariableName(), "ecs");
		withSystemProperties(properties,
				() -> assertThat(fileAppenderLayout())
					.asInstanceOf(InstanceOfAssertFactories.type(StructuredLogLayout.class))
					.extracting(StructuredLogLayout::getCharset, InstanceOfAssertFactories.type(Charset.class))
					.isEqualTo(StandardCharsets.UTF_8));
	}

	@Override
	protected String getConfigFileName() {
		return "log4j2-file.xml";
	}

	@Override
	protected void prepareConfiguration() {
		System.setProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName(),
				new File(this.temp, "test.log").getAbsolutePath());
		super.prepareConfiguration();
	}

	private Layout<? extends Serializable> fileAppenderLayout() {
		prepareConfiguration();
		assertThat(this.configuration).isNotNull();
		return this.configuration.getAppender("File").getLayout();
	}

}
