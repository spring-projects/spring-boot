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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.CronTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.config.json.JsonConfigurationFactory;
import org.apache.logging.log4j.core.config.plugins.util.PluginRegistry;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationBuilder;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationFactory;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.yaml.YamlConfigurationFactory;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.jul.Log4jBridgeHandler;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;

import org.springframework.boot.logging.AbstractLoggingSystemTests;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.testsupport.logging.ConfigureClasspathToPreferLog4j2;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link Log4J2LoggingSystem}.
 *
 * @author Daniel Fullarton
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Ben Hale
 * @author Madhura Bhave
 * @author Piotr P. Karwasz
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathExclusions("logback-*.jar")
@ConfigureClasspathToPreferLog4j2
class Log4J2LoggingSystemTests extends AbstractLoggingSystemTests {

	private TestLog4J2LoggingSystem loggingSystem;

	private MockEnvironment environment;

	private LoggingInitializationContext initializationContext;

	private Logger logger;

	private Configuration configuration;

	private String contextName;

	@BeforeEach
	void setup() {
		PluginRegistry.getInstance().clear();
		this.loggingSystem = new TestLog4J2LoggingSystem();
		this.environment = new MockEnvironment();
		this.initializationContext = new LoggingInitializationContext(this.environment);
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		this.configuration = loggerContext.getConfiguration();
		this.loggingSystem.cleanUp();
		this.logger = LogManager.getLogger(getClass());
		this.contextName = loggerContext.getName();
	}

	@AfterEach
	void cleanUp() {
		this.loggingSystem.cleanUp();
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		loggerContext.stop();
		loggerContext.start(((Reconfigurable) this.configuration).reconfigure());
		PluginRegistry.getInstance().clear();
	}

	@Test
	void noFile(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		Configuration configuration = this.loggingSystem.getConfiguration();
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(new File(tmpDir() + "/spring.log")).doesNotExist();
		assertThat(configuration.getConfigurationSource().getFile()).isNotNull();
	}

	@Test
	void withFile(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(this.initializationContext, getRelativeClasspathLocation("log4j2-file.xml"),
				getLogFile(null, tmpDir()));
		this.logger.info("Hello world");
		Configuration configuration = this.loggingSystem.getConfiguration();
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(new File(tmpDir() + "/spring.log")).exists();
		assertThat(configuration.getConfigurationSource().getLocation()).contains("log4j2-file.xml");
	}

	@Test
	@WithNonDefaultXmlResource
	void testNonDefaultConfigLocation(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, "classpath:nondefault.xml",
				getLogFile(tmpDir() + "/tmp.log", null));
		this.logger.info("Hello world");
		Configuration configuration = this.loggingSystem.getConfiguration();
		assertThat(output).contains("Hello world").contains(tmpDir() + "/tmp.log");
		assertThat(new File(tmpDir() + "/tmp.log")).doesNotExist();
		assertThat(configuration.getConfigurationSource().getFile().getAbsolutePath()).contains("nondefault.xml");
		assertThat(configuration.getWatchManager().getIntervalSeconds()).isEqualTo(30);
	}

	@Test
	void testNonexistentConfigLocation() {
		this.loggingSystem.beforeInitialize();
		assertThatIllegalStateException().isThrownBy(() -> this.loggingSystem.initialize(this.initializationContext,
				"classpath:log4j2-nonexistent.xml", null));
	}

	@Test
	void getSupportedLevels() {
		assertThat(this.loggingSystem.getSupportedLogLevels()).isEqualTo(EnumSet.allOf(LogLevel.class));
	}

	@Test
	void setLevel(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.debug("Hello");
		assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isOne();
	}

	@Test
	void setLevelToNull(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", null);
		this.logger.debug("Hello");
		assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isOne();
	}

	@Test
	void getLoggerConfigurations() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		List<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
		assertThat(configurations).isNotEmpty();
		assertThat(configurations.get(0).getName()).isEqualTo(LoggingSystem.ROOT_LOGGER_NAME);
	}

	@Test
	void getLoggerConfigurationsShouldReturnAllLoggers() {
		LogManager.getLogger("org.springframework.boot.logging.log4j2.Log4J2LoggingSystemTests$Nested");
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		List<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
		assertThat(configurations).isNotEmpty();
		assertThat(configurations.get(0).getName()).isEqualTo(LoggingSystem.ROOT_LOGGER_NAME);
		Map<String, LogLevel> loggers = new LinkedHashMap<>();
		configurations.forEach((logger) -> loggers.put(logger.getName(), logger.getConfiguredLevel()));
		assertIsPresent("org", loggers, null);
		assertIsPresent("org.springframework.boot.logging.log4j2", loggers, null);
		assertIsPresent("org.springframework.boot.logging.log4j2.Log4J2LoggingSystemTests", loggers, LogLevel.DEBUG);
		assertIsPresent("org.springframework.boot.logging.log4j2.Log4J2LoggingSystemTests$Nested", loggers, null);
	}

	@Test // gh-35227
	void getLoggerConfigurationWhenHasCustomLevel() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		String loggerName = getClass().getName();
		org.apache.logging.log4j.Level level = org.apache.logging.log4j.Level.forName("CUSTOM_LEVEL", 1000);
		loggerContext.getConfiguration().addLogger(loggerName, new LoggerConfig(loggerName, level, true));
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(loggerName);
		assertThat(configuration.getLevelConfiguration().getName()).isEqualTo("CUSTOM_LEVEL");
	}

	private void assertIsPresent(String loggerName, Map<String, LogLevel> loggers, LogLevel logLevel) {
		assertThat(loggers).containsKey(loggerName);
		assertThat(loggers).containsEntry(loggerName, logLevel);
	}

	@Test
	void getLoggerConfiguration() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(getClass().getName());
		assertThat(configuration)
			.isEqualTo(new LoggerConfiguration(getClass().getName(), LogLevel.DEBUG, LogLevel.DEBUG));
	}

	@Test
	void getLoggerConfigurationShouldReturnLoggerWithNullConfiguredLevel() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration("org");
		assertThat(configuration).isEqualTo(new LoggerConfiguration("org", null, LogLevel.INFO));
	}

	@Test
	void getLoggerConfigurationForNonExistentLoggerShouldReturnNull() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration("doesnotexist");
		assertThat(configuration).isNull();
	}

	@Test
	void setLevelOfUnconfiguredLoggerDoesNotAffectRootConfiguration(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		LogManager.getRootLogger().debug("Hello");
		this.loggingSystem.setLogLevel("foo.bar.baz", LogLevel.DEBUG);
		LogManager.getRootLogger().debug("Hello");
		assertThat(output.toString()).doesNotContain("Hello");
	}

	@Test
	void loggingThatUsesJulIsCaptured(CapturedOutput output) {
		String name = getClass().getName();
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(name, LogLevel.TRACE);
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(name);
		julLogger.setLevel(java.util.logging.Level.INFO);
		julLogger.severe("Hello world");
		assertThat(output).contains("Hello world");
	}

	static Stream<String> configLocationsWithConfigurationFileSystemProperty() {
		return Stream.of("log4j2.configurationFile", "log4j.configuration.location");
	}

	@ParameterizedTest
	@MethodSource
	void configLocationsWithConfigurationFileSystemProperty(String propertyName) {
		System.setProperty(propertyName, "custom-log4j2.properties");
		try {
			assertThat(this.loggingSystem.getStandardConfigLocations()).containsExactly("custom-log4j2.properties",
					"log4j2-test" + this.contextName + ".xml", "log4j2-test.xml", "log4j2" + this.contextName + ".xml",
					"log4j2.xml");
		}
		finally {
			System.clearProperty(propertyName);
		}
	}

	static Stream<Arguments> standardConfigLocations() {
		// For each configuration file format we make "available" to the
		// Log4j2LoggingSystem:
		// - The Log4j Core `ConfigurationFactory` class
		// - The tree parser used internally by that configuration factory
		return Stream.of(
				// No classes, only XML
				Arguments.of(Collections.emptyList(), List.of(".xml")),
				// Log4j Core 2
				Arguments.of(List.of(JsonConfigurationFactory.class.getName(),
						"com.fasterxml.jackson.databind.ObjectMapper"), List.of(".json", ".jsn", ".xml")),
				Arguments.of(List.of(PropertiesConfigurationFactory.class.getName(),
						PropertiesConfigurationBuilder.class.getName()), List.of(".properties", ".xml")),
				Arguments.of(List.of(YamlConfigurationFactory.class.getName(),
						"com.fasterxml.jackson.dataformat.yaml.YAMLMapper"), List.of(".yaml", ".yml", ".xml")),
				Arguments.of(List.of(JsonConfigurationFactory.class.getName(),
						"com.fasterxml.jackson.databind.ObjectMapper", PropertiesConfigurationFactory.class.getName(),
						PropertiesConfigurationBuilder.class.getName(), YamlConfigurationFactory.class.getName(),
						"com.fasterxml.jackson.dataformat.yaml.YAMLMapper"),
						List.of(".properties", ".yaml", ".yml", ".json", ".jsn", ".xml")),
				// Log4j Core 3
				Arguments.of(List.of(JsonConfigurationFactory.class.getName(),
						"org.apache.logging.log4j.kit.json.JsonReader"), List.of(".json", ".jsn", ".xml")),
				Arguments.of(List.of("org.apache.logging.log4j.config.properties.JavaPropsConfigurationFactory",
						"tools.jackson.dataformat.javaprop.JavaPropsMapper"), List.of(".properties", ".xml")),
				Arguments.of(List.of("org.apache.logging.log4j.config.yaml.YamlConfigurationFactory",
						"tools.jackson.dataformat.yaml.YAMLMapper"), List.of(".yaml", ".yml", ".xml")),
				Arguments.of(
						List.of(JsonConfigurationFactory.class.getName(),
								"org.apache.logging.log4j.kit.json.JsonReader",
								"org.apache.logging.log4j.config.properties.JavaPropsConfigurationFactory",
								"tools.jackson.dataformat.javaprop.JavaPropsMapper",
								"org.apache.logging.log4j.config.yaml.YamlConfigurationFactory",
								"tools.jackson.dataformat.yaml.YAMLMapper"),
						List.of(".properties", ".yaml", ".yml", ".json", ".jsn", ".xml")));
	}

	@ParameterizedTest
	@MethodSource
	void standardConfigLocations(List<String> availableClasses, List<String> expectedSuffixes) {
		this.loggingSystem.availableClasses(availableClasses.toArray(new String[0]));
		String[] locations = this.loggingSystem.getStandardConfigLocations();
		assertThat(locations).hasSize(4 * expectedSuffixes.size());
		List<String> expected = new ArrayList<>();
		expectedSuffixes.forEach((s) -> expected.add("log4j2-test" + this.contextName + s));
		expectedSuffixes.forEach((s) -> expected.add("log4j2-test" + s));
		expectedSuffixes.forEach((s) -> expected.add("log4j2" + this.contextName + s));
		expectedSuffixes.forEach((s) -> expected.add("log4j2" + s));
		assertThat(locations).containsExactlyElementsOf(expected);
	}

	@Test
	void springConfigLocations() {
		String[] locations = getSpringConfigLocations(this.loggingSystem);
		assertThat(locations).containsExactly("log4j2-test" + this.contextName + "-spring.xml",
				"log4j2-test-spring.xml", "log4j2" + this.contextName + "-spring.xml", "log4j2-spring.xml");
	}

	@Test
	void exceptionsIncludeClassPackaging(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, getRelativeClasspathLocation("log4j2-file.xml"),
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
		this.loggingSystem.initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
	}

	@Test
	void customExceptionConversionWord(CapturedOutput output) {
		System.setProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.getEnvironmentVariableName(), "%ex");
		try {
			this.loggingSystem.beforeInitialize();
			this.logger.info("Hidden");
			this.loggingSystem.initialize(this.initializationContext, getRelativeClasspathLocation("log4j2-file.xml"),
					getLogFile(null, tmpDir()));
			this.logger.warn("Expected exception", new RuntimeException("Expected", new RuntimeException("Cause")));
			String fileContents = contentOf(new File(tmpDir() + "/spring.log"));
			assertThat(fileContents).contains("java.lang.RuntimeException: Expected").doesNotContain("Wrapped by:");
			assertThat(output).contains("java.lang.RuntimeException: Expected").doesNotContain("Wrapped by:");
		}
		finally {
			System.clearProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.getEnvironmentVariableName());
		}
	}

	@Test
	void initializationIsOnlyPerformedOnceUntilCleanedUp() {
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		PropertyChangeListener listener = mock(PropertyChangeListener.class);
		loggerContext.addPropertyChangeListener(listener);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		then(listener).should(times(2)).propertyChange(any(PropertyChangeEvent.class));
		this.loggingSystem.cleanUp();
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		then(listener).should(times(4)).propertyChange(any(PropertyChangeEvent.class));
	}

	@Test
	void getLoggerConfigurationWithResetLevelReturnsNull() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel("com.example", LogLevel.WARN);
		this.loggingSystem.setLogLevel("com.example.test", LogLevel.DEBUG);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration("com.example.test");
		assertThat(configuration)
			.isEqualTo(new LoggerConfiguration("com.example.test", LogLevel.DEBUG, LogLevel.DEBUG));
		this.loggingSystem.setLogLevel("com.example.test", null);
		LoggerConfiguration updatedConfiguration = this.loggingSystem.getLoggerConfiguration("com.example.test");
		assertThat(updatedConfiguration).isNull();
	}

	@Test
	void getLoggerConfigurationWithResetLevelWhenAlreadyConfiguredReturnsParentConfiguredLevel() {
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		loggerContext.getConfiguration()
			.addLogger("com.example.test",
					new LoggerConfig("com.example.test", org.apache.logging.log4j.Level.INFO, false));
		this.loggingSystem.setLogLevel("com.example", LogLevel.WARN);
		this.loggingSystem.setLogLevel("com.example.test", LogLevel.DEBUG);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration("com.example.test");
		assertThat(configuration)
			.isEqualTo(new LoggerConfiguration("com.example.test", LogLevel.DEBUG, LogLevel.DEBUG));
		this.loggingSystem.setLogLevel("com.example.test", null);
		LoggerConfiguration updatedConfiguration = this.loggingSystem.getLoggerConfiguration("com.example.test");
		assertThat(updatedConfiguration)
			.isEqualTo(new LoggerConfiguration("com.example.test", LogLevel.WARN, LogLevel.WARN));
	}

	@Test
	void log4jLevelsArePropagatedToJul() {
		this.loggingSystem.beforeInitialize();
		java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
		// check if Log4jBridgeHandler is used
		Handler[] handlers = rootLogger.getHandlers();
		assertThat(handlers).hasSize(1);
		assertThat(handlers[0]).isInstanceOf(Log4jBridgeHandler.class);

		this.loggingSystem.initialize(this.initializationContext, null, null);
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Log4J2LoggingSystemTests.class.getName());
		logger.info("Log to trigger level propagation");
		assertThat(logger.getLevel()).isNull();
		this.loggingSystem.setLogLevel(Log4J2LoggingSystemTests.class.getName(), LogLevel.DEBUG);
		assertThat(logger.getLevel()).isEqualTo(Level.FINE);
	}

	@Test
	void shutdownHookIsDisabled() {
		assertThat(
				PropertiesUtil.getProperties().getBooleanProperty(ShutdownCallbackRegistry.SHUTDOWN_HOOK_ENABLED, true))
			.isFalse();
	}

	@Test
	@WithNonDefaultXmlResource
	void loadOptionalOverrideConfigurationWhenDoesNotExist() {
		this.environment.setProperty("logging.log4j2.config.override", "optional:classpath:override.xml");
		this.loggingSystem.initialize(this.initializationContext, "classpath:nondefault.xml", null);
		assertThat(this.loggingSystem.getConfiguration()).isInstanceOf(XmlConfiguration.class);
	}

	@Test
	void loadOptionalOverrideConfigurationWhenDoesNotExistUponReinitialization() {
		this.environment.setProperty("logging.log4j2.config.override", "optional:classpath:override.xml");
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		assertThat(this.loggingSystem.getConfiguration()).isInstanceOf(XmlConfiguration.class);
		this.loggingSystem.cleanUp();
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		assertThat(this.loggingSystem.getConfiguration()).isInstanceOf(XmlConfiguration.class);
	}

	@Test
	@WithNonDefaultXmlResource
	@WithOverrideXmlResource
	void loadOptionalOverrideConfiguration() {
		this.environment.setProperty("logging.log4j2.config.override", "optional:classpath:override.xml");
		this.loggingSystem.initialize(this.initializationContext, "classpath:nondefault.xml", null);
		assertThat(this.loggingSystem.getConfiguration()).isInstanceOf(CompositeConfiguration.class);
	}

	@Test
	@WithOverrideXmlResource
	void loadOptionalOverrideConfigurationUponReinitialization() {
		this.environment.setProperty("logging.log4j2.config.override", "optional:classpath:override.xml");
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		assertThat(this.loggingSystem.getConfiguration()).isInstanceOf(CompositeConfiguration.class);
		this.loggingSystem.cleanUp();
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		assertThat(this.loggingSystem.getConfiguration()).isInstanceOf(CompositeConfiguration.class);
	}

	@Test
	@WithNonDefaultXmlResource
	@WithOverrideXmlResource
	void compositeConfigurationWithCustomBaseConfiguration() {
		this.environment.setProperty("logging.log4j2.config.override", "classpath:override.xml");
		this.loggingSystem.initialize(this.initializationContext, "classpath:nondefault.xml", null);
		assertThat(this.loggingSystem.getConfiguration()).isInstanceOf(CompositeConfiguration.class);
	}

	@Test
	@WithOverrideXmlResource
	void compositeConfigurationWithStandardConfigLocationConfiguration() {
		this.environment.setProperty("logging.log4j2.config.override", "classpath:override.xml");
		this.loggingSystem.initialize(this.initializationContext, null, null);
		assertThat(this.loggingSystem.getConfiguration()).isInstanceOf(CompositeConfiguration.class);
	}

	@Test
	void initializeAttachesEnvironmentToLoggerContext() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		Environment environment = Log4J2LoggingSystem.getEnvironment(loggerContext);
		assertThat(environment).isSameAs(this.environment);
	}

	@Test
	void initializeRegisterStatusListenerAndAttachToLoggerContext() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		StatusListener statusListener = (StatusListener) loggerContext
			.getObject(Log4J2LoggingSystem.STATUS_LISTENER_KEY);
		assertThat(statusListener).isNotNull();
		assertThat(StatusLogger.getLogger().getListeners()).contains(statusListener);
	}

	@Test
	void statusListenerIsUpdatedUponReinitialization() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		// listener should be registered
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		StatusListener statusListener = (StatusListener) loggerContext
			.getObject(Log4J2LoggingSystem.STATUS_LISTENER_KEY);
		assertThat(statusListener).isNotNull();
		assertThat(StatusLogger.getLogger().getListeners()).contains(statusListener);

		this.loggingSystem.cleanUp();
		// listener should be removed from context and StatusLogger
		assertThat(StatusLogger.getLogger().getListeners()).doesNotContain(statusListener);
		assertThat(loggerContext.getObject(Log4J2LoggingSystem.STATUS_LISTENER_KEY)).isNull();

		// a new listener should be registered
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		StatusListener statusListener1 = (StatusListener) loggerContext
			.getObject(Log4J2LoggingSystem.STATUS_LISTENER_KEY);
		assertThat(statusListener1).isNotNull();
		assertThat(StatusLogger.getLogger().getListeners()).contains(statusListener1);
	}

	@Test
	void initializeAddsSpringEnvironmentPropertySource() {
		this.environment.setProperty("spring", "boot");
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		PropertiesUtil properties = PropertiesUtil.getProperties();
		assertThat(properties.getStringProperty("spring")).isEqualTo("boot");
	}

	@Test
	void environmentIsUpdatedUponReinitialization() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("spring", "boot: one");
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(new LoggingInitializationContext(environment), null, null);
		assertThat(PropertiesUtil.getProperties().getStringProperty("spring")).isEqualTo("boot: one");
		this.loggingSystem.cleanUp();
		this.environment.setProperty("spring", "boot: two");
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		assertThat(PropertiesUtil.getProperties().getStringProperty("spring")).isEqualTo("boot: two");
	}

	@Test
	void nonFileUrlsAreResolvedUsingLog4J2UrlConnectionFactory() {
		this.loggingSystem.beforeInitialize();
		assertThatIllegalStateException()
			.isThrownBy(() -> this.loggingSystem.initialize(this.initializationContext,
					"http://localhost:8080/shouldnotwork", null))
			.havingCause()
			.isInstanceOf(ProtocolException.class)
			.withMessageContaining("http has not been enabled");
	}

	@Test
	void correlationLoggingToFileWhenExpectCorrelationIdTrueAndMdcContent() {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
		new LoggingSystemProperties(this.environment).apply();
		File file = new File(tmpDir(), "log4j2-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world"))
			.contains(" [01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void correlationLoggingToConsoleWhenExpectCorrelationIdTrueAndMdcContent(CapturedOutput output) {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world"))
			.contains(" [01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void correlationLoggingToConsoleWhenExpectCorrelationIdFalseAndMdcContent(CapturedOutput output) {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "false");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).doesNotContain("0123456789012345");
	}

	@Test
	void correlationLoggingToConsoleWhenExpectCorrelationIdTrueAndNoMdcContent(CapturedOutput output) {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world"))
			.contains(" [                                                 ] ");
	}

	@Test
	void correlationLoggingToConsoleWhenHasCorrelationPattern(CapturedOutput output) {
		this.environment.setProperty("logging.pattern.correlation", "%correlationId{spanId(0),traceId(0)}");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world"))
			.contains(" [0123456789012345-01234567890123456789012345678901] ");
	}

	@Test
	void applicationNameLoggingToConsoleWhenHasApplicationName(CapturedOutput output) {
		this.environment.setProperty("spring.application.name", "myapp");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("[myapp] ");
	}

	@Test
	void applicationNameLoggingToConsoleWhenHasApplicationNameWithParenthesis(CapturedOutput output) {
		this.environment.setProperty("spring.application.name", "myapp (dev)");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("[myapp (dev)] ");
	}

	@Test
	void applicationNameLoggingToConsoleWhenDisabled(CapturedOutput output) {
		this.environment.setProperty("spring.application.name", "myapp");
		this.environment.setProperty("logging.include-application-name", "false");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).doesNotContain("${sys:LOGGED_APPLICATION_NAME}")
			.doesNotContain("${sys:APPLICATION_NAME}")
			.doesNotContain("myapp");
	}

	@Test
	void applicationNameLoggingToFileWhenHasApplicationName() {
		this.environment.setProperty("spring.application.name", "myapp");
		new LoggingSystemProperties(this.environment).apply();
		File file = new File(tmpDir(), "log4j2-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("[myapp] ");
	}

	@Test
	void applicationNameLoggingToFileWhenHasApplicationNameWithParenthesis() {
		this.environment.setProperty("spring.application.name", "myapp (dev)");
		new LoggingSystemProperties(this.environment).apply();
		File file = new File(tmpDir(), "log4j2-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("[myapp (dev)] ");
	}

	@Test
	void applicationNameLoggingToFileWhenDisabled() {
		this.environment.setProperty("spring.application.name", "myapp");
		this.environment.setProperty("logging.include-application-name", "false");
		new LoggingSystemProperties(this.environment).apply();
		File file = new File(tmpDir(), "log4j2-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).doesNotContain("${sys:LOGGED_APPLICATION_NAME}")
			.doesNotContain("${sys:APPLICATION_NAME}")
			.doesNotContain("myapp");
	}

	@Test
	void applicationGroupLoggingToConsoleWhenHasApplicationGroup(CapturedOutput output) {
		this.environment.setProperty("spring.application.group", "mygroup");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("[mygroup] ");
	}

	@Test
	void applicationGroupLoggingToConsoleWhenHasApplicationGroupWithParenthesis(CapturedOutput output) {
		this.environment.setProperty("spring.application.group", "mygroup (dev)");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("[mygroup (dev)] ");
	}

	@Test
	void applicationGroupLoggingToConsoleWhenDisabled(CapturedOutput output) {
		this.environment.setProperty("spring.application.group", "mygroup");
		this.environment.setProperty("logging.include-application-group", "false");
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).doesNotContain("${sys:APPLICATION_GROUP}")
			.doesNotContain("mygroup");
	}

	@Test
	void applicationGroupLoggingToFileWhenHasApplicationGroup() {
		this.environment.setProperty("spring.application.group", "mygroup");
		new LoggingSystemProperties(this.environment).apply();
		File file = new File(tmpDir(), "log4j2-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("[mygroup] ");
	}

	@Test
	void applicationGroupLoggingToFileWhenHasApplicationGroupWithParenthesis() {
		this.environment.setProperty("spring.application.group", "mygroup (dev)");
		new LoggingSystemProperties(this.environment).apply();
		File file = new File(tmpDir(), "log4j2-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("[mygroup (dev)] ");
	}

	@Test
	void applicationGroupLoggingToFileWhenDisabled() {
		this.environment.setProperty("spring.application.group", "mygroup");
		this.environment.setProperty("logging.include-application-group", "false");
		new LoggingSystemProperties(this.environment).apply();
		File file = new File(tmpDir(), "log4j2-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.setStandardConfigLocations(false);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).doesNotContain("${sys:APPLICATION_GROUP}")
			.doesNotContain("mygroup");
	}

	@Test
	void shouldNotContainAnsiEscapeCodes(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(output).doesNotContain("\033[");
	}

	private String getRelativeClasspathLocation(String fileName) {
		String defaultPath = ClassUtils.getPackageName(getClass());
		defaultPath = defaultPath.replace('.', '/');
		defaultPath = defaultPath + "/" + fileName;
		defaultPath = "classpath:" + defaultPath;
		return defaultPath;
	}

	/**
	 * Used for testing that loggers in nested classes are returned by
	 * {@link Log4J2LoggingSystem#getLoggerConfigurations()} .
	 */
	static class Nested {

		@SuppressWarnings("unused")
		private static final Log logger = LogFactory.getLog(Nested.class);

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "nondefault.xml",
			content = """
					<Configuration status="WARN" monitorInterval="30">
						<Properties>
							<Property name="PID">????</Property>
							<Property name="LOG_PATTERN">${sys:LOG_FILE} %d{yyyy-MM-dd HH:mm:ss.SSS}] service%X{context} - ${sys:PID} %5p [%t] --- %c{1}: %m%n</Property>
						</Properties>
						<Appenders>
							<Console name="Console" target="SYSTEM_OUT" follow="true">
								<PatternLayout pattern="${LOG_PATTERN}"/>
							</Console>
						</Appenders>
						<Loggers>
							<Root level="info">
								<AppenderRef ref="Console"/>
							</Root>
						</Loggers>
					</Configuration>
					""")
	private @interface WithNonDefaultXmlResource {

	}

	@Test
	void rollingPolicySystemPropertiesAreApplied() {
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-file-size", "50MB");
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-history", "30");
		this.environment.setProperty("logging.log4j2.rollingpolicy.clean-history-on-start", "true");
		this.environment.setProperty("logging.log4j2.rollingpolicy.total-size-cap", "5GB");
		this.environment.setProperty("logging.log4j2.rollingpolicy.file-name-pattern",
				"${LOG_FILE}.%d{yyyy-MM-dd}.%i.log");
		File file = new File(tmpDir(), "log4j2-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		String maxFileSize = System.getProperty("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE");
		String maxHistory = System.getProperty("LOG4J2_ROLLINGPOLICY_MAX_HISTORY");
		String cleanHistoryOnStart = System.getProperty("LOG4J2_ROLLINGPOLICY_CLEAN_HISTORY_ON_START");
		String totalSizeCap = System.getProperty("LOG4J2_ROLLINGPOLICY_TOTAL_SIZE_CAP");
		String fileNamePattern = System.getProperty("LOG4J2_ROLLINGPOLICY_FILE_NAME_PATTERN");
		assertThat(maxFileSize).isEqualTo(String.valueOf(50 * 1024 * 1024));
		assertThat(maxHistory).isEqualTo("30");
		assertThat(cleanHistoryOnStart).isEqualTo("true");
		assertThat(totalSizeCap).isEqualTo(String.valueOf(DataSize.ofGigabytes(5).toBytes()));
		assertThat(fileNamePattern).isEqualTo("${LOG_FILE}.%d{yyyy-MM-dd}.%i.log");
	}

	@Test
	void rollingPolicyDeprecatedPropertiesAreApplied() {
		this.environment.setProperty("logging.file.max-size", "20MB");
		this.environment.setProperty("logging.file.max-history", "15");
		File file = new File(tmpDir(), "log4j2-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		String maxFileSize = System.getProperty("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE");
		String maxHistory = System.getProperty("LOG4J2_ROLLINGPOLICY_MAX_HISTORY");
		assertThat(maxFileSize).isEqualTo(String.valueOf(20 * 1024 * 1024));
		assertThat(maxHistory).isEqualTo("15");
	}

	@Test
	void rollingPolicyTimeStrategyIsApplied() {
		this.environment.setProperty("logging.log4j2.rollingpolicy.strategy", "time");
		this.environment.setProperty("logging.log4j2.rollingpolicy.time-based.interval", "2");
		SpringBootTriggeringPolicy policy = getTriggeringPolicy();
		TriggeringPolicy delegate = policy.getDelegate();
		assertThat(delegate).isInstanceOf(TimeBasedTriggeringPolicy.class);
		TimeBasedTriggeringPolicy timePolicy = (TimeBasedTriggeringPolicy) delegate;
		assertThat(timePolicy.getInterval()).isEqualTo(2);
	}

	@Test
	void rollingPolicySizeAndTimeStrategyIsApplied() {
		this.environment.setProperty("logging.log4j2.rollingpolicy.strategy", "size-and-time");
		this.environment.setProperty("logging.log4j2.rollingpolicy.max-file-size", "5MB");
		this.environment.setProperty("logging.log4j2.rollingpolicy.time-based.interval", "3");
		SpringBootTriggeringPolicy policy = getTriggeringPolicy();
		TriggeringPolicy delegate = policy.getDelegate();
		assertThat(delegate).isInstanceOf(CompositeTriggeringPolicy.class);
		CompositeTriggeringPolicy composite = (CompositeTriggeringPolicy) delegate;
		TriggeringPolicy[] policies = composite.getTriggeringPolicies();
		assertThat(policies).anyMatch(TimeBasedTriggeringPolicy.class::isInstance);
		assertThat(policies).anyMatch(SizeBasedTriggeringPolicy.class::isInstance);
	}

	@Test
	void rollingPolicyCronStrategyIsApplied() {
		this.environment.setProperty("logging.log4j2.rollingpolicy.strategy", "cron");
		this.environment.setProperty("logging.log4j2.rollingpolicy.cron.schedule", "0 0 0 * * ?");
		SpringBootTriggeringPolicy policy = getTriggeringPolicy();
		TriggeringPolicy delegate = policy.getDelegate();
		assertThat(delegate).isInstanceOf(CronTriggeringPolicy.class);
		CronTriggeringPolicy cronPolicy = (CronTriggeringPolicy) delegate;
		assertThat(cronPolicy.getCronExpression().getCronExpression()).isEqualTo("0 0 0 * * ?");
	}

	private SpringBootTriggeringPolicy getTriggeringPolicy() {
		File file = new File(tmpDir(), "target-file.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, "classpath:org/springframework/boot/logging/log4j2/log4j2-file.xml", logFile);
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		Configuration configuration = loggerContext.getConfiguration();
		System.out.println("Configuration location: " + configuration.getConfigurationSource().getLocation());
		RollingFileAppender appender = (RollingFileAppender) configuration.getAppenders().get("File");
		assertThat(appender).isNotNull();
			TriggeringPolicy topPolicy = appender.getManager().getTriggeringPolicy();
			SpringBootTriggeringPolicy policy = findSpringBootTriggeringPolicy(topPolicy);
			assertThat(policy).isNotNull();
			return policy;
		}

		private SpringBootTriggeringPolicy findSpringBootTriggeringPolicy(TriggeringPolicy policy) {
			if (policy instanceof SpringBootTriggeringPolicy springBoot) {
				return springBoot;
			}
			if (policy instanceof CompositeTriggeringPolicy composite) {
				for (TriggeringPolicy child : composite.getTriggeringPolicies()) {
					SpringBootTriggeringPolicy found = findSpringBootTriggeringPolicy(child);
					if (found != null) {
						return found;
					}
				}
			}
			return null;
		}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "override.xml", content = """
			<?xml version="1.0" encoding="UTF-8"?>
			<Configuration status="WARN" monitorInterval="30">
				<Appenders>
					<Console name="Console" target="SYSTEM_OUT" follow="true">
						<PatternLayout pattern="${LOG_PATTERN}"/>
					</Console>
				</Appenders>
			</Configuration>
			""")
	private @interface WithOverrideXmlResource {

	}

}
