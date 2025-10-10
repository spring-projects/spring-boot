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

import org.apache.logging.log4j.core.layout.PatternLayout;
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
		assertThat(fileAppenderPattern()).contains("%xwEx");
	}

	@Test
	void whenLogExceptionConversionWordIsSetThenFileAppenderUsesIt() {
		withSystemProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.getEnvironmentVariableName(), "custom",
				() -> assertThat(fileAppenderPattern()).contains("custom"));
	}

	@Test
	void whenLogLevelPatternIsNotConfiguredThenFileAppenderUsesDefault() {
		assertThat(fileAppenderPattern()).contains("%5p");
	}

	@Test
	void whenLogLevelPatternIsSetThenFileAppenderUsesIt() {
		withSystemProperty(LoggingSystemProperty.LEVEL_PATTERN.getEnvironmentVariableName(), "custom",
				() -> assertThat(fileAppenderPattern()).contains("custom"));
	}

	@Test
	void whenLogLDateformatPatternIsNotConfiguredThenFileAppenderUsesDefault() {
		assertThat(fileAppenderPattern()).contains("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	}

	@Test
	void whenLogDateformatPatternIsSetThenFileAppenderUsesIt() {
		withSystemProperty(LoggingSystemProperty.DATEFORMAT_PATTERN.getEnvironmentVariableName(), "dd-MM-yyyy",
				() -> assertThat(fileAppenderPattern()).contains("dd-MM-yyyy"));
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

	private String fileAppenderPattern() {
		prepareConfiguration();
		assertThat(this.configuration).isNotNull();
		return ((PatternLayout) this.configuration.getAppender("File").getLayout()).getConversionPattern();
	}

}
