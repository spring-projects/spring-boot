/*
 * Copyright 2012-2019 the original author or authors.
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Reconfigurable;
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
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link Log4J2LoggingSystem}.
 *
 * @author Daniel Fullarton
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Ben Hale
 * @author Madhura Bhave
 */
@ExtendWith(OutputCaptureExtension.class)
class Log4J2LoggingSystemTests extends AbstractLoggingSystemTests {

	private final TestLog4J2LoggingSystem loggingSystem = new TestLog4J2LoggingSystem();

	private Logger logger;

	private Configuration configuration;

	@BeforeEach
	void setup() {
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		this.configuration = loggerContext.getConfiguration();
		this.loggingSystem.cleanUp();
		this.logger = LogManager.getLogger(getClass());
	}

	@AfterEach
	void cleanUp() {
		this.loggingSystem.cleanUp();
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		loggerContext.stop();
		loggerContext.start(((Reconfigurable) this.configuration).reconfigure());
	}

	@Test
	void noFile(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(null, null, null);
		this.logger.info("Hello world");
		Configuration configuration = this.loggingSystem.getConfiguration();
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(new File(tmpDir() + "/spring.log").exists()).isFalse();
		assertThat(configuration.getConfigurationSource().getFile()).isNotNull();
	}

	@Test
	void withFile(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(null, getRelativeClasspathLocation("log4j2-file.xml"),
				getLogFile(null, tmpDir()));
		this.logger.info("Hello world");
		Configuration configuration = this.loggingSystem.getConfiguration();
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(new File(tmpDir() + "/spring.log").exists()).isTrue();
		assertThat(configuration.getConfigurationSource().getFile()).isNotNull();
	}

	@Test
	void testNonDefaultConfigLocation(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, "classpath:log4j2-nondefault.xml", getLogFile(tmpDir() + "/tmp.log", null));
		this.logger.info("Hello world");
		Configuration configuration = this.loggingSystem.getConfiguration();
		assertThat(output).contains("Hello world").contains(tmpDir() + "/tmp.log");
		assertThat(new File(tmpDir() + "/tmp.log").exists()).isFalse();
		assertThat(configuration.getConfigurationSource().getFile().getAbsolutePath())
				.contains("log4j2-nondefault.xml");
		assertThat(configuration.getWatchManager().getIntervalSeconds()).isEqualTo(30);
	}

	@Test
	void testNonexistentConfigLocation() {
		this.loggingSystem.beforeInitialize();
		assertThatIllegalStateException()
				.isThrownBy(() -> this.loggingSystem.initialize(null, "classpath:log4j2-nonexistent.xml", null));
	}

	@Test
	void getSupportedLevels() {
		assertThat(this.loggingSystem.getSupportedLogLevels()).isEqualTo(EnumSet.allOf(LogLevel.class));
	}

	@Test
	void setLevel(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.debug("Hello");
		assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isEqualTo(1);
	}

	@Test
	void setLevelToNull(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", null);
		this.logger.debug("Hello");
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

	@Test
	void setLevelOfUnconfiguredLoggerDoesNotAffectRootConfiguration(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		LogManager.getRootLogger().debug("Hello");
		this.loggingSystem.setLogLevel("foo.bar.baz", LogLevel.DEBUG);
		LogManager.getRootLogger().debug("Hello");
		assertThat(output.toString()).doesNotContain("Hello");
	}

	@Test
	void loggingThatUsesJulIsCaptured(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(getClass().getName());
		julLogger.severe("Hello world");
		assertThat(output).contains("Hello world");
	}

	@Test
	void configLocationsWithNoExtraDependencies() {
		assertThat(this.loggingSystem.getStandardConfigLocations()).contains("log4j2-test.properties",
				"log4j2-test.xml", "log4j2.properties", "log4j2.xml");
	}

	@Test
	void configLocationsWithJacksonDatabind() {
		this.loggingSystem.availableClasses(ObjectMapper.class.getName());
		assertThat(this.loggingSystem.getStandardConfigLocations()).containsExactly("log4j2-test.properties",
				"log4j2-test.json", "log4j2-test.jsn", "log4j2-test.xml", "log4j2.properties", "log4j2.json",
				"log4j2.jsn", "log4j2.xml");
	}

	@Test
	void configLocationsWithJacksonDataformatYaml() {
		this.loggingSystem.availableClasses("com.fasterxml.jackson.dataformat.yaml.YAMLParser");
		assertThat(this.loggingSystem.getStandardConfigLocations()).containsExactly("log4j2-test.properties",
				"log4j2-test.yaml", "log4j2-test.yml", "log4j2-test.xml", "log4j2.properties", "log4j2.yaml",
				"log4j2.yml", "log4j2.xml");
	}

	@Test
	void configLocationsWithJacksonDatabindAndDataformatYaml() {
		this.loggingSystem.availableClasses("com.fasterxml.jackson.dataformat.yaml.YAMLParser",
				ObjectMapper.class.getName());
		assertThat(this.loggingSystem.getStandardConfigLocations()).containsExactly("log4j2-test.properties",
				"log4j2-test.yaml", "log4j2-test.yml", "log4j2-test.json", "log4j2-test.jsn", "log4j2-test.xml",
				"log4j2.properties", "log4j2.yaml", "log4j2.yml", "log4j2.json", "log4j2.jsn", "log4j2.xml");
	}

	@Test
	void springConfigLocations() {
		String[] locations = getSpringConfigLocations(this.loggingSystem);
		assertThat(locations).containsExactly("log4j2-test-spring.properties", "log4j2-test-spring.xml",
				"log4j2-spring.properties", "log4j2-spring.xml");
	}

	@Test
	void exceptionsIncludeClassPackaging(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, getRelativeClasspathLocation("log4j2-file.xml"),
				getLogFile(null, tmpDir()));
		this.logger.warn("Expected exception", new RuntimeException("Expected"));
		String fileContents = contentOf(new File(tmpDir() + "/spring.log"));
		assertThat(fileContents).contains("[junit-");
		assertThat(output).contains("[junit-");
	}

	@Test
	void beforeInitializeFilterDisablesErrorLogging() {
		this.loggingSystem.beforeInitialize();
		assertThat(this.logger.isErrorEnabled()).isFalse();
		this.loggingSystem.initialize(null, null, getLogFile(null, tmpDir()));
	}

	@Test
	void customExceptionConversionWord(CapturedOutput output) {
		System.setProperty(LoggingSystemProperties.EXCEPTION_CONVERSION_WORD, "%ex");
		try {
			this.loggingSystem.beforeInitialize();
			this.logger.info("Hidden");
			this.loggingSystem.initialize(null, getRelativeClasspathLocation("log4j2-file.xml"),
					getLogFile(null, tmpDir()));
			this.logger.warn("Expected exception", new RuntimeException("Expected", new RuntimeException("Cause")));
			String fileContents = contentOf(new File(tmpDir() + "/spring.log"));
			assertThat(fileContents).contains("java.lang.RuntimeException: Expected").doesNotContain("Wrapped by:");
			assertThat(output).contains("java.lang.RuntimeException: Expected").doesNotContain("Wrapped by:");
		}
		finally {
			System.clearProperty(LoggingSystemProperties.EXCEPTION_CONVERSION_WORD);
		}
	}

	@Test
	void initializationIsOnlyPerformedOnceUntilCleanedUp() {
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		PropertyChangeListener listener = mock(PropertyChangeListener.class);
		loggerContext.addPropertyChangeListener(listener);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		verify(listener, times(2)).propertyChange(any(PropertyChangeEvent.class));
		this.loggingSystem.cleanUp();
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null, null);
		verify(listener, times(4)).propertyChange(any(PropertyChangeEvent.class));
	}

	private String getRelativeClasspathLocation(String fileName) {
		String defaultPath = ClassUtils.getPackageName(getClass());
		defaultPath = defaultPath.replace('.', '/');
		defaultPath = defaultPath + "/" + fileName;
		defaultPath = "classpath:" + defaultPath;
		return defaultPath;
	}

	static class TestLog4J2LoggingSystem extends Log4J2LoggingSystem {

		private List<String> availableClasses = new ArrayList<>();

		TestLog4J2LoggingSystem() {
			super(TestLog4J2LoggingSystem.class.getClassLoader());
		}

		Configuration getConfiguration() {
			return ((org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false)).getConfiguration();
		}

		@Override
		protected boolean isClassAvailable(String className) {
			return this.availableClasses.contains(className);
		}

		private void availableClasses(String... classNames) {
			Collections.addAll(this.availableClasses, classNames);
		}

	}

}
