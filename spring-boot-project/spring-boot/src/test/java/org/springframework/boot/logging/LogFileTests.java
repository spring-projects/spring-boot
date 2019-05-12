/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.Test;

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
public class LogFileTests {

	@Test
	public void noProperties() {
		PropertyResolver resolver = getPropertyResolver(Collections.emptyMap());
		LogFile logFile = LogFile.get(resolver);
		assertThat(logFile).isNull();
	}

	@Test
	public void loggingFile() {
		PropertyResolver resolver = getPropertyResolver(
				Collections.singletonMap("logging.file.name", "log.file"));
		testLoggingFile(resolver);
	}

	@Test
	@Deprecated
	public void loggingFileWithDeprecatedProperties() {
		PropertyResolver resolver = getPropertyResolver(
				Collections.singletonMap("logging.file", "log.file"));
		testLoggingFile(resolver);
	}

	private void testLoggingFile(PropertyResolver resolver) {
		LogFile logFile = LogFile.get(resolver);
		Properties properties = new Properties();
		logFile.applyTo(properties);
		assertThat(logFile.toString()).isEqualTo("log.file");
		assertThat(properties.getProperty(LoggingSystemProperties.LOG_FILE))
				.isEqualTo("log.file");
		assertThat(properties.getProperty(LoggingSystemProperties.LOG_PATH)).isNull();
	}

	@Test
	public void loggingPath() {
		PropertyResolver resolver = getPropertyResolver(
				Collections.singletonMap("logging.file.path", "logpath"));
		testLoggingPath(resolver);
	}

	@Test
	@Deprecated
	public void loggingPathWithDeprecatedProperties() {
		PropertyResolver resolver = getPropertyResolver(
				Collections.singletonMap("logging.path", "logpath"));
		testLoggingPath(resolver);
	}

	private void testLoggingPath(PropertyResolver resolver) {
		LogFile logFile = LogFile.get(resolver);
		Properties properties = new Properties();
		logFile.applyTo(properties);
		assertThat(logFile.toString())
				.isEqualTo("logpath" + File.separatorChar + "spring.log");
		assertThat(properties.getProperty(LoggingSystemProperties.LOG_FILE))
				.isEqualTo("logpath" + File.separatorChar + "spring.log");
		assertThat(properties.getProperty(LoggingSystemProperties.LOG_PATH))
				.isEqualTo("logpath");
	}

	@Test
	public void loggingFileAndPath() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("logging.file.name", "log.file");
		properties.put("logging.file.path", "logpath");
		PropertyResolver resolver = getPropertyResolver(properties);
		testLoggingFileAndPath(resolver);
	}

	@Test
	@Deprecated
	public void loggingFileAndPathWithDeprecatedProperties() {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("logging.file", "log.file");
		properties.put("logging.path", "logpath");
		PropertyResolver resolver = getPropertyResolver(properties);
		testLoggingFileAndPath(resolver);
	}

	private void testLoggingFileAndPath(PropertyResolver resolver) {
		LogFile logFile = LogFile.get(resolver);
		Properties properties = new Properties();
		logFile.applyTo(properties);
		assertThat(logFile.toString()).isEqualTo("log.file");
		assertThat(properties.getProperty(LoggingSystemProperties.LOG_FILE))
				.isEqualTo("log.file");
		assertThat(properties.getProperty(LoggingSystemProperties.LOG_PATH))
				.isEqualTo("logpath");
	}

	private PropertyResolver getPropertyResolver(Map<String, Object> properties) {
		PropertySource<?> propertySource = new MapPropertySource("properties",
				properties);
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(propertySource);
		return new PropertySourcesPropertyResolver(propertySources);
	}

}
