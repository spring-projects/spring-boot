/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.logging.java;

import java.io.File;
import java.io.FileFilter;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.logging.AbstractLoggingSystemTests;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JavaLoggingSystem}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Ben Hale
 */
@ExtendWith(OutputCaptureExtension.class)
class JavaLoggingSystemTests extends AbstractLoggingSystemTests {

	private static final FileFilter SPRING_LOG_FILTER = (pathname) -> pathname.getName().startsWith("spring.log");

	private final JavaLoggingSystem loggingSystem = new JavaLoggingSystem(getClass().getClassLoader());

	private Logger logger;

	private Locale defaultLocale;

	@BeforeEach
	void init() {
		this.logger = Logger.getLogger(getClass().getName());
		this.defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.ENGLISH);
	}

	@AfterEach
	void resetLogger() {
		this.logger.setLevel(Level.OFF);
		this.loggingSystem.getShutdownHandler().run();
	}

	@AfterEach
	void restoreLocale() {
		Locale.setDefault(this.defaultLocale);
	}

	@Test
	void noFile(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(null, null, null);
		this.logger.info("Hello world");
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(new File(tmpDir() + "/spring.log").exists()).isFalse();
	}

	@Test
	void withFile(CapturedOutput output) {
		File temp = new File(tmpDir());
		File[] logFiles = temp.listFiles(SPRING_LOG_FILTER);
		for (File file : logFiles) {
			file.delete();
		}
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(null, null, getLogFile(null, tmpDir()));
		this.logger.info("Hello world");
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(temp.listFiles(SPRING_LOG_FILTER)).hasSizeGreaterThan(0);
	}

	@Test
	void testCustomFormatter(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		this.logger.info("Hello world");
		assertThat(output).contains("Hello world").contains("???? INFO [");
	}

	@Test
	void testSystemPropertyInitializesFormat(CapturedOutput output) {
		System.setProperty(LoggingSystemProperties.PID_KEY, "1234");
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null,
				"classpath:" + ClassUtils.addResourcePathToPackagePath(getClass(), "logging.properties"), null);
		this.logger.info("Hello world");
		this.logger.info("Hello world");
		assertThat(output).contains("Hello world").contains("1234 INFO [");
	}

	@Test
	void testNonDefaultConfigLocation(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, "classpath:logging-nondefault.properties", null);
		this.logger.info("Hello world");
		assertThat(output).contains("INFO: Hello");
	}

	@Test
	void testNonexistentConfigLocation() {
		this.loggingSystem.beforeInitialize();
		assertThatIllegalStateException().isThrownBy(
				() -> this.loggingSystem.initialize(null, "classpath:logging-nonexistent.properties", null));
	}

	@Test
	void getSupportedLevels() {
		assertThat(this.loggingSystem.getSupportedLogLevels()).isEqualTo(
				EnumSet.of(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.OFF));
	}

	@Test
	void setLevel(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		this.logger.fine("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.fine("Hello");
		assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isEqualTo(1);
	}

	@Test
	void setLevelToNull(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		this.logger.fine("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.fine("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", null);
		this.logger.fine("Hello");
		assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isEqualTo(1);
	}

	@Test
	void getLoggingConfigurations() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		List<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
		assertThat(configurations).isNotEmpty();
		assertThat(configurations.get(0).getName()).isEqualTo(LoggingSystem.ROOT_LOGGER_NAME);
	}

	@Test
	void getLoggingConfiguration() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(getClass().getName());
		assertThat(configuration)
				.isEqualTo(new LoggerConfiguration(getClass().getName(), LogLevel.DEBUG, LogLevel.DEBUG));
	}

}
