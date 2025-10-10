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

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogFile}.
 *
 * @author Phillip Webb
 */
class LogFileTests {

	@Test
	void noProperties() {
		PropertyResolver resolver = getPropertyResolver(Collections.emptyMap());
		LogFile logFile = LogFile.get(resolver);
		assertThat(logFile).isNull();
	}

	@Test
	void loggingFile() {
		PropertyResolver resolver = getPropertyResolver(Collections.singletonMap("logging.file.name", "log.file"));
		testLoggingFile(resolver);
	}

	private void testLoggingFile(PropertyResolver resolver) {
		LogFile logFile = LogFile.get(resolver);
		assertThat(logFile).isNotNull();
		Properties properties = new Properties();
		logFile.applyTo(properties);
		assertThat(logFile).hasToString("log.file");
		assertThat(properties.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
			.isEqualTo("log.file");
		assertThat(properties.getProperty(LoggingSystemProperty.LOG_PATH.getEnvironmentVariableName())).isNull();
	}

	@Test
	void loggingPath() {
		PropertyResolver resolver = getPropertyResolver(Collections.singletonMap("logging.file.path", "logpath"));
		testLoggingPath(resolver);
	}

	private void testLoggingPath(PropertyResolver resolver) {
		LogFile logFile = LogFile.get(resolver);
		assertThat(logFile).isNotNull();
		Properties properties = new Properties();
		logFile.applyTo(properties);
		assertThat(logFile).hasToString("logpath" + File.separatorChar + "spring.log");
		assertThat(properties.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
			.isEqualTo("logpath" + File.separatorChar + "spring.log");
		assertThat(properties.getProperty(LoggingSystemProperty.LOG_PATH.getEnvironmentVariableName()))
			.isEqualTo("logpath");
	}

	@Test
	void loggingFileAndPath() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("logging.file.name", "log.file");
		properties.put("logging.file.path", "logpath");
		PropertyResolver resolver = getPropertyResolver(properties);
		testLoggingFileAndPath(resolver);
	}

	private void testLoggingFileAndPath(PropertyResolver resolver) {
		LogFile logFile = LogFile.get(resolver);
		assertThat(logFile).isNotNull();
		Properties properties = new Properties();
		logFile.applyTo(properties);
		assertThat(logFile).hasToString("log.file");
		assertThat(properties.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
			.isEqualTo("log.file");
		assertThat(properties.getProperty(LoggingSystemProperty.LOG_PATH.getEnvironmentVariableName()))
			.isEqualTo("logpath");
	}

	private PropertyResolver getPropertyResolver(Map<String, Object> properties) {
		PropertySource<?> propertySource = new MapPropertySource("properties", properties);
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(propertySource);
		return new PropertySourcesPropertyResolver(propertySources);
	}

}
