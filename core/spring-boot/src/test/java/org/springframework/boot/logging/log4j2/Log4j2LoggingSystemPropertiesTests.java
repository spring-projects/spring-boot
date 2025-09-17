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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.logging.LogFile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Log4j2LoggingSystemProperties}.
 *
 * @author hojooo
 */
class Log4j2LoggingSystemPropertiesTests {

	private final Map<String, String> systemProperties = new LinkedHashMap<>();

	private final BiConsumer<String, String> systemPropertySetter = this.systemProperties::put;

	@AfterEach
	void clearSystemProperties() {
		this.systemProperties.clear();
	}

	@Test
	void appliesLog4j2RollingPolicyProperties() {
		Environment environment = new StandardEnvironment();
		addPropertiesToEnvironment(environment, "logging.log4j2.rollingpolicy.max-file-size", "50MB",
				"logging.log4j2.rollingpolicy.clean-history-on-start", "true",
				"logging.log4j2.rollingpolicy.max-history", "30",
				"logging.log4j2.rollingpolicy.total-size-cap", "10GB",
				"logging.log4j2.rollingpolicy.file-name-pattern", "test.%d{yyyy-MM-dd}.%i.log");
		new Log4j2LoggingSystemProperties(environment, this.systemPropertySetter).apply(null);
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE",
				String.valueOf(DataSize.ofMegabytes(50).toBytes()));
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_CLEAN_HISTORY_ON_START", "true");
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_MAX_HISTORY", "30");
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_TOTAL_SIZE_CAP",
				String.valueOf(DataSize.ofGigabytes(10).toBytes()));
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_FILE_NAME_PATTERN",
				"test.%d{yyyy-MM-dd}.%i.log");
	}

	@Test
	void appliesLog4j2RollingPolicyPropertiesWithDefaults() {
		Environment environment = new StandardEnvironment();
		new Log4j2LoggingSystemProperties(environment, this.systemPropertySetter).apply(null);
		// Should not set any rolling policy properties when not configured
		assertThat(this.systemProperties.keySet()).noneMatch((key) -> key.startsWith("LOG4J2_ROLLINGPOLICY"));
	}

	@Test
	void appliesDeprecatedProperties() {
		Environment environment = new StandardEnvironment();
		addPropertiesToEnvironment(environment, "logging.file.max-size", "20MB", "logging.file.max-history", "15");
		new Log4j2LoggingSystemProperties(environment, this.systemPropertySetter).apply(null);
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE",
				String.valueOf(DataSize.ofMegabytes(20).toBytes()));
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_MAX_HISTORY", "15");
	}

	@Test
	void newPropertiesOverrideDeprecatedProperties() {
		Environment environment = new StandardEnvironment();
		addPropertiesToEnvironment(environment, "logging.log4j2.rollingpolicy.max-file-size", "100MB",
				"logging.file.max-size", "20MB", "logging.log4j2.rollingpolicy.max-history", "50",
				"logging.file.max-history", "15");
		new Log4j2LoggingSystemProperties(environment, this.systemPropertySetter).apply(null);
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE",
				String.valueOf(DataSize.ofMegabytes(100).toBytes()));
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_MAX_HISTORY", "50");
	}

	@Test
	void appliesWithLogFile() {
		Environment environment = new StandardEnvironment();
		addPropertiesToEnvironment(environment, "logging.log4j2.rollingpolicy.max-file-size", "25MB");
		LogFile logFile = LogFile.get(environment);
		new Log4j2LoggingSystemProperties(environment, this.systemPropertySetter).apply(logFile);
		assertThat(this.systemProperties).containsEntry("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE",
				String.valueOf(DataSize.ofMegabytes(25).toBytes()));
	}

	private void addPropertiesToEnvironment(Environment environment, String... pairs) {
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < pairs.length; i += 2) {
			map.put(pairs[i], pairs[i + 1]);
		}
		((StandardEnvironment) environment).getPropertySources()
			.addFirst(new MapPropertySource("test", map));
	}

}