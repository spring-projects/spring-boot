/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoggingSystemProperties}.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Jonatan Ivanov
 */
class LoggingSystemPropertiesTests {

	private Set<Object> systemPropertyNames;

	@BeforeEach
	void captureSystemPropertyNames() {
		for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
			System.getProperties().remove(property.getEnvironmentVariableName());
		}
		this.systemPropertyNames = new HashSet<>(System.getProperties().keySet());
	}

	@AfterEach
	void restoreSystemProperties() {
		System.getProperties().keySet().retainAll(this.systemPropertyNames);
	}

	@Test
	void pidIsSet() {
		new LoggingSystemProperties(new MockEnvironment()).apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.PID)).isNotNull();
	}

	@Test
	void consoleLogPatternIsSet() {
		new LoggingSystemProperties(new MockEnvironment().withProperty("logging.pattern.console", "console pattern"))
			.apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN)).isEqualTo("console pattern");
	}

	@Test
	void consoleCharsetWhenNoPropertyUsesUtf8() {
		new LoggingSystemProperties(new MockEnvironment()).apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_CHARSET)).isEqualTo("UTF-8");
	}

	@Test
	void consoleCharsetIsSet() {
		new LoggingSystemProperties(new MockEnvironment().withProperty("logging.charset.console", "UTF-16"))
			.apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_CHARSET)).isEqualTo("UTF-16");
	}

	@Test
	void fileLogPatternIsSet() {
		new LoggingSystemProperties(new MockEnvironment().withProperty("logging.pattern.file", "file pattern"))
			.apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.FILE_PATTERN)).isEqualTo("file pattern");
	}

	@Test
	void fileCharsetWhenNoPropertyUsesUtf8() {
		new LoggingSystemProperties(new MockEnvironment()).apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.FILE_CHARSET)).isEqualTo("UTF-8");
	}

	@Test
	void fileCharsetIsSet() {
		new LoggingSystemProperties(new MockEnvironment().withProperty("logging.charset.file", "UTF-16")).apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.FILE_CHARSET)).isEqualTo("UTF-16");
	}

	@Test
	void consoleLogPatternCanReferencePid() {
		new LoggingSystemProperties(environment("logging.pattern.console", "${PID:unknown}")).apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.CONSOLE_PATTERN)).matches("[0-9]+");
	}

	@Test
	void fileLogPatternCanReferencePid() {
		new LoggingSystemProperties(environment("logging.pattern.file", "${PID:unknown}")).apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.FILE_PATTERN)).matches("[0-9]+");
	}

	private String getSystemProperty(LoggingSystemProperty property) {
		return System.getProperty(property.getEnvironmentVariableName());
	}

	@Test
	void correlationPatternIsSet() {
		new LoggingSystemProperties(
				new MockEnvironment().withProperty("logging.pattern.correlation", "correlation pattern"))
			.apply(null);
		assertThat(System.getProperty(LoggingSystemProperty.CORRELATION_PATTERN.getEnvironmentVariableName()))
			.isEqualTo("correlation pattern");
	}

	@Test
	void defaultValueResolverIsUsed() {
		MockEnvironment environment = new MockEnvironment();
		Map<String, String> defaultValues = Map
			.of(LoggingSystemProperty.CORRELATION_PATTERN.getApplicationPropertyName(), "default correlation pattern");
		new LoggingSystemProperties(environment, defaultValues::get, null).apply(null);
		assertThat(System.getProperty(LoggingSystemProperty.CORRELATION_PATTERN.getEnvironmentVariableName()))
			.isEqualTo("default correlation pattern");
	}

	@Test
	void loggedApplicationNameWhenHasApplicationName() {
		new LoggingSystemProperties(new MockEnvironment().withProperty("spring.application.name", "test")).apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.APPLICATION_NAME)).isEqualTo("[test] ");
	}

	@Test
	void loggedApplicationNameWhenHasNoApplicationName() {
		new LoggingSystemProperties(new MockEnvironment()).apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.APPLICATION_NAME)).isNull();
	}

	@Test
	void loggedApplicationNameWhenApplicationNameLoggingDisabled() {
		new LoggingSystemProperties(new MockEnvironment().withProperty("spring.application.name", "test")
			.withProperty("logging.include-application-name", "false")).apply(null);
		assertThat(getSystemProperty(LoggingSystemProperty.APPLICATION_NAME)).isNull();
	}

	private Environment environment(String key, Object value) {
		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(new MapPropertySource("test", Collections.singletonMap(key, value)));
		return environment;
	}

}
