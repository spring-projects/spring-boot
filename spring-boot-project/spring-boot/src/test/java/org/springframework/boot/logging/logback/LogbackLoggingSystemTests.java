/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.stream.Stream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.OnConsoleStatusListener;
import ch.qos.logback.core.status.StatusManager;
import ch.qos.logback.core.status.WarnStatus;
import ch.qos.logback.core.util.DynamicClassLoadingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.bridge.SLF4JBridgeHandler;

import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.logging.AbstractLoggingSystemTests;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.boot.logging.LoggingSystemProperty;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.contentOf;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link LogbackLoggingSystem}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Ben Hale
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @author Eddú Meléndez
 * @author Scott Frederick
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathExclusions({ "log4j-core-*.jar", "log4j-api-*.jar" })
class LogbackLoggingSystemTests extends AbstractLoggingSystemTests {

	private final LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(getClass().getClassLoader());

	private Logger logger;

	private MockEnvironment environment;

	private LoggingInitializationContext initializationContext;

	private Set<Object> systemPropertyNames;

	@BeforeEach
	void setup() {
		for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
			System.getProperties().remove(property.getEnvironmentVariableName());
		}
		this.systemPropertyNames = new HashSet<>(System.getProperties().keySet());
		this.loggingSystem.cleanUp();
		this.logger = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger(getClass());
		this.environment = new MockEnvironment();
		ConversionService conversionService = ApplicationConversionService.getSharedInstance();
		this.environment.setConversionService((ConfigurableConversionService) conversionService);
		this.initializationContext = new LoggingInitializationContext(this.environment);
		this.loggingSystem.setStatusPrinterStream(System.out);
	}

	@AfterEach
	void cleanUp() {
		System.getProperties().keySet().retainAll(this.systemPropertyNames);
		this.loggingSystem.cleanUp();
		((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
	}

	@Test
	void logbackDefaultsConfigurationDoesNotTriggerDeprecation(CapturedOutput output) {
		initialize(this.initializationContext, "classpath:logback-include-defaults.xml", null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).isEqualTo("[INFO] - Hello world");
		assertThat(output.toString()).doesNotContain("WARN").doesNotContain("deprecated");
	}

	@Test
	void logbackBaseConfigurationDoesNotTriggerDeprecation(CapturedOutput output) {
		initialize(this.initializationContext, "classpath:logback-include-base.xml", null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains(" INFO ").endsWith(": Hello world");
		assertThat(output.toString()).doesNotContain("WARN").doesNotContain("deprecated");
	}

	@Test
	@ClassPathOverrides({ "org.jboss.logging:jboss-logging:3.5.0.Final", "org.apache.logging.log4j:log4j-core:2.19.0" })
	void jbossLoggingRoutesThroughLog4j2ByDefault() {
		System.getProperties().remove("org.jboss.logging.provider");
		org.jboss.logging.Logger jbossLogger = org.jboss.logging.Logger.getLogger(getClass());
		assertThat(jbossLogger.getClass().getName()).isEqualTo("org.jboss.logging.Log4j2Logger");
	}

	@Test
	@ClassPathOverrides("org.jboss.logging:jboss-logging:3.5.0.Final")
	void jbossLoggingRoutesThroughSlf4jWhenLoggingSystemIsInitialized() {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		assertThat(org.jboss.logging.Logger.getLogger(getClass()).getClass().getName())
			.isEqualTo("org.jboss.logging.Slf4jLocationAwareLogger");
	}

	@Test
	void noFile(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(getLineWithText(output, "Hello world")).contains("INFO");
		assertThat(new File(tmpDir() + "/spring.log")).doesNotExist();
	}

	@Test
	void withFile(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
		this.logger.info("Hello world");
		File file = new File(tmpDir() + "/spring.log");
		assertThat(output).doesNotContain("LOGBACK:");
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(getLineWithText(output, "Hello world")).contains("INFO");
		assertThat(file).exists();
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "maxFileSize")).hasToString("10 MB");
		assertThat(getRollingPolicy().getMaxHistory()).isEqualTo(7);
	}

	@Test
	void defaultConfigConfiguresAConsoleAppender() {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		assertThat(getConsoleAppender()).isNotNull();
	}

	@Test
	void testNonDefaultConfigLocation(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, "classpath:logback-nondefault.xml",
				getLogFile(tmpDir() + "/tmp.log", null));
		this.logger.info("Hello world");
		assertThat(output).doesNotContain("DEBUG")
			.contains("Hello world")
			.contains(tmpDir() + "/tmp.log")
			.endsWith("BOOTBOOT");
		assertThat(new File(tmpDir() + "/tmp.log")).doesNotExist();
	}

	@Test
	void testLogbackSpecificSystemProperty(CapturedOutput output) {
		System.setProperty("logback.configurationFile", "/foo/my-file.xml");
		try {
			this.loggingSystem.beforeInitialize();
			initialize(this.initializationContext, null, null);
			assertThat(output)
				.contains("Ignoring 'logback.configurationFile' system property. Please use 'logging.config' instead.");
		}
		finally {
			System.clearProperty("logback.configurationFile");
		}
	}

	@Test
	void testNonexistentConfigLocation() {
		this.loggingSystem.beforeInitialize();
		assertThatIllegalStateException()
			.isThrownBy(() -> initialize(this.initializationContext, "classpath:logback-nonexistent.xml", null));
	}

	@Test
	void getSupportedLevels() {
		assertThat(this.loggingSystem.getSupportedLogLevels()).isEqualTo(
				EnumSet.of(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.OFF));
	}

	@Test
	void setLevel(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.debug("Hello");
		assertThat(StringUtils.countOccurrencesOf(output.toString(), "Hello")).isOne();
	}

	@Test
	void setLevelToNull(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
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
		initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		List<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
		assertThat(configurations).isNotEmpty();
		assertThat(configurations.get(0).getName()).isEqualTo(LoggingSystem.ROOT_LOGGER_NAME);
	}

	@Test
	void getLoggerConfiguration() {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(getClass().getName());
		assertThat(configuration)
			.isEqualTo(new LoggerConfiguration(getClass().getName(), LogLevel.DEBUG, LogLevel.DEBUG));
	}

	@Test
	void getLoggerConfigurationForLoggerThatDoesNotExistShouldReturnNull() {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration("doesnotexist");
		assertThat(configuration).isNull();
	}

	@Test
	@Deprecated(since = "3.3.5", forRemoval = true)
	void getLoggerConfigurationForALL() {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		Logger logger = (Logger) LoggerFactory.getILoggerFactory().getLogger(getClass().getName());
		logger.setLevel(Level.ALL);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(getClass().getName());
		assertThat(configuration)
			.isEqualTo(new LoggerConfiguration(getClass().getName(), LogLevel.TRACE, LogLevel.TRACE));
	}

	@Test
	void systemLevelTraceShouldReturnNativeLevelTraceNotAll() {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.TRACE);
		Logger logger = (Logger) LoggerFactory.getILoggerFactory().getLogger(getClass().getName());
		assertThat(logger.getLevel()).isEqualTo(Level.TRACE);
	}

	@Test
	void loggingThatUsesJulIsCaptured(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(getClass().getName());
		julLogger.info("Hello world");
		assertThat(output).contains("Hello world");
	}

	@Test
	void loggingLevelIsPropagatedToJul(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(getClass().getName());
		julLogger.fine("Hello debug world");
		assertThat(output).contains("Hello debug world");
	}

	@Test
	void bridgeHandlerLifecycle() {
		assertThat(bridgeHandlerInstalled()).isFalse();
		this.loggingSystem.beforeInitialize();
		assertThat(bridgeHandlerInstalled()).isTrue();
		this.loggingSystem.cleanUp();
		assertThat(bridgeHandlerInstalled()).isFalse();
	}

	@Test
	void standardConfigLocations() {
		String[] locations = this.loggingSystem.getStandardConfigLocations();
		assertThat(locations).containsExactly("logback-test.groovy", "logback-test.xml", "logback.groovy",
				"logback.xml");
	}

	@Test
	void springConfigLocations() {
		String[] locations = getSpringConfigLocations(this.loggingSystem);
		assertThat(locations).containsExactly("logback-test-spring.groovy", "logback-test-spring.xml",
				"logback-spring.groovy", "logback-spring.xml");
	}

	private boolean bridgeHandlerInstalled() {
		java.util.logging.Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		for (Handler handler : handlers) {
			if (handler instanceof SLF4JBridgeHandler) {
				return true;
			}
		}
		return false;
	}

	@Test
	void testConsolePatternProperty(CapturedOutput output) {
		this.environment.setProperty("logging.pattern.console", "%logger %msg");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		initialize(loggingInitializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).doesNotContain("INFO");
	}

	@Test
	void testLevelPatternProperty(CapturedOutput output) {
		this.environment.setProperty("logging.pattern.level", "X%clr(%p)X");
		new LoggingSystemProperties(this.environment).apply();
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		initialize(loggingInitializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("XINFOX");
	}

	@Test
	void testFilePatternProperty(CapturedOutput output) {
		this.environment.setProperty("logging.pattern.file", "%logger %msg");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("INFO");
		assertThat(getLineWithText(file, "Hello world")).doesNotContain("INFO");
	}

	@Test
	void testCleanHistoryOnStartProperty() {
		this.environment.setProperty("logging.file.clean-history-on-start", "true");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(getRollingPolicy().isCleanHistoryOnStart()).isTrue();
	}

	@Test
	void testCleanHistoryOnStartPropertyWithXmlConfiguration() {
		this.environment.setProperty("logging.file.clean-history-on-start", "true");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, "classpath:logback-include-base.xml", logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(getRollingPolicy().isCleanHistoryOnStart()).isTrue();
	}

	@Test
	void testMaxFileSizePropertyWithLogbackFileSize() {
		testMaxFileSizeProperty("100 MB", "100 MB");
	}

	@Test
	void testMaxFileSizePropertyWithDataSize() {
		testMaxFileSizeProperty("15MB", "15 MB");
	}

	@Test
	void testMaxFileSizePropertyWithBytesValue() {
		testMaxFileSizeProperty(String.valueOf(10 * 1024 * 1024), "10 MB");
	}

	private void testMaxFileSizeProperty(String sizeValue, String expectedFileSize) {
		this.environment.setProperty("logging.file.max-size", sizeValue);
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "maxFileSize")).hasToString(expectedFileSize);
	}

	@Test
	void testMaxFileSizePropertyWithXmlConfiguration() {
		this.environment.setProperty("logging.file.max-size", "100MB");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, "classpath:logback-include-base.xml", logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "maxFileSize")).hasToString("100 MB");
	}

	@Test
	void testMaxHistoryProperty() {
		this.environment.setProperty("logging.file.max-history", "30");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(getRollingPolicy().getMaxHistory()).isEqualTo(30);
	}

	@Test
	void testMaxHistoryPropertyWithXmlConfiguration() {
		this.environment.setProperty("logging.file.max-history", "30");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, "classpath:logback-include-base.xml", logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(getRollingPolicy().getMaxHistory()).isEqualTo(30);
	}

	@Test
	void testTotalSizeCapPropertyWithLogbackFileSize() {
		testTotalSizeCapProperty("101 MB", "101 MB");
	}

	@Test
	void testTotalSizeCapPropertyWithDataSize() {
		testTotalSizeCapProperty("10MB", "10 MB");
	}

	@Test
	void testTotalSizeCapPropertyWithBytesValue() {
		testTotalSizeCapProperty(String.valueOf(10 * 1024 * 1024), "10 MB");
	}

	private void testTotalSizeCapProperty(String sizeValue, String expectedFileSize) {
		this.environment.setProperty("logging.file.total-size-cap", sizeValue);
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "totalSizeCap")).hasToString(expectedFileSize);
	}

	@Test
	void testTotalSizeCapPropertyWithXmlConfiguration() {
		String expectedSize = "101 MB";
		this.environment.setProperty("logging.file.total-size-cap", expectedSize);
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, "classpath:logback-include-base.xml", logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "totalSizeCap")).hasToString(expectedSize);
	}

	@Test
	void exceptionsIncludeClassPackaging(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
		this.logger.warn("Expected exception", new RuntimeException("Expected"));
		String fileContents = contentOf(new File(tmpDir() + "/spring.log"));
		assertThat(fileContents).contains("[junit-");
		assertThat(output).contains("[junit-");
	}

	@Test
	void customExceptionConversionWord(CapturedOutput output) {
		System.setProperty(LoggingSystemProperty.EXCEPTION_CONVERSION_WORD.getEnvironmentVariableName(), "%ex");
		try {
			this.loggingSystem.beforeInitialize();
			this.logger.info("Hidden");
			initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
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
	void initializeShouldSetSystemProperty() {
		// gh-5491
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		LogFile logFile = getLogFile(tmpDir() + "/example.log", null, false);
		initialize(this.initializationContext, "classpath:logback-nondefault.xml", logFile);
		assertThat(System.getProperty(LoggingSystemProperty.LOG_FILE.getEnvironmentVariableName()))
			.endsWith("example.log");
	}

	@Test
	void initializeShouldApplyLogbackSystemPropertiesToTheContext() {
		this.environment.setProperty("logging.logback.rollingpolicy.file-name-pattern", "file-name-pattern");
		this.environment.setProperty("logging.logback.rollingpolicy.clean-history-on-start", "true");
		this.environment.setProperty("logging.logback.rollingpolicy.max-file-size", "10MB");
		this.environment.setProperty("logging.logback.rollingpolicy.total-size-cap", "100MB");
		this.environment.setProperty("logging.logback.rollingpolicy.max-history", "20");
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Map<String, String> properties = loggerContext.getCopyOfPropertyMap();
		Set<String> expectedProperties = new HashSet<>();
		Stream.of(RollingPolicySystemProperty.values())
			.map(RollingPolicySystemProperty::getEnvironmentVariableName)
			.forEach(expectedProperties::add);
		Stream.of(LoggingSystemProperty.values())
			.map(LoggingSystemProperty::getEnvironmentVariableName)
			.forEach(expectedProperties::add);
		expectedProperties.removeAll(List.of("LOG_FILE", "LOG_PATH"));
		expectedProperties.add("org.jboss.logging.provider");
		expectedProperties.add("LOG_CORRELATION_PATTERN");
		expectedProperties.add("CONSOLE_LOG_STRUCTURED_FORMAT");
		expectedProperties.add("FILE_LOG_STRUCTURED_FORMAT");
		assertThat(properties).containsOnlyKeys(expectedProperties);
		assertThat(properties).containsEntry("CONSOLE_LOG_CHARSET", Charset.defaultCharset().name());
	}

	@Test
	void initializationIsOnlyPerformedOnceUntilCleanedUp() {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		LoggerContextListener listener = mock(LoggerContextListener.class);
		loggerContext.addListener(listener);
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		then(listener).should().onReset(loggerContext);
		this.loggingSystem.cleanUp();
		loggerContext.addListener(listener);
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		then(listener).should(times(2)).onReset(loggerContext);
	}

	@Test
	void testDateformatPatternDefault(CapturedOutput output) {
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		initialize(loggingInitializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world"))
			.containsPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}([-+]\\d{2}:\\d{2}|Z)");
	}

	@Test
	void testDateformatPatternProperty(CapturedOutput output) {
		this.environment.setProperty("logging.pattern.dateformat", "dd-MM-yyyy");
		new LoggingSystemProperties(this.environment).apply();
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		initialize(loggingInitializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).containsPattern("\\d{2}-\\d{2}-\\d{4}\\s");
	}

	@Test // gh-24835
	void testDateformatPatternPropertyDirect(CapturedOutput output) {
		this.environment.setProperty("logging.pattern.dateformat", "yyyy");
		new LoggingSystemProperties(this.environment).apply();
		this.environment.setProperty("logging.pattern.dateformat", "dd-MM-yyyy");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		initialize(loggingInitializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).containsPattern("\\d{2}-\\d{2}-\\d{4}\\s");
	}

	@Test
	void noDebugOutputIsProducedByDefault(CapturedOutput output) {
		System.clearProperty("logback.debug");
		this.loggingSystem.beforeInitialize();
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(this.initializationContext, null, logFile);
		assertThat(output).doesNotContain("LevelChangePropagator").doesNotContain("SizeAndTimeBasedFNATP");
	}

	@Test
	void logbackDebugPropertyIsHonored(CapturedOutput output) {
		System.setProperty("logback.debug", "true");
		try {
			this.loggingSystem.beforeInitialize();
			LoggerContext loggerContext = this.logger.getLoggerContext();
			StatusManager statusManager = loggerContext.getStatusManager();
			statusManager.add(new InfoStatus("INFO STATUS MESSAGE", getClass()));
			statusManager.add(new WarnStatus("WARN STATUS MESSAGE", getClass()));
			statusManager.add(new ErrorStatus("ERROR STATUS MESSAGE", getClass()));
			File file = new File(tmpDir(), "logback-test.log");
			LogFile logFile = getLogFile(file.getPath(), null);
			initialize(this.initializationContext, null, logFile);
			assertThat(output).contains("LevelChangePropagator")
				.contains("SizeAndTimeBasedFileNamingAndTriggeringPolicy")
				.contains("DebugLogbackConfigurator")
				.contains("INFO STATUS MESSAGE")
				.contains("WARN STATUS MESSAGE")
				.contains("ERROR STATUS MESSAGE");
			assertThat(loggerContext.getStatusManager().getCopyOfStatusListenerList()).allSatisfy((listener) -> {
				assertThat(listener).isInstanceOf(SystemStatusListener.class);
				assertThat(listener).hasFieldOrPropertyWithValue("debug", true);
			});
		}
		finally {
			System.clearProperty("logback.debug");
		}
	}

	@Test
	void logbackSystemStatusListenerShouldBeRegisteredWhenCustomLogbackXmlHasStatusListener(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, "classpath:logback-include-status-listener.xml", null);
		LoggerContext loggerContext = this.logger.getLoggerContext();
		assertThat(loggerContext.getStatusManager().getCopyOfStatusListenerList()).hasSize(2)
			.allSatisfy((listener) -> assertThat(listener).isInstanceOf(OnConsoleStatusListener.class))
			.anySatisfy((listener) -> assertThat(listener).isInstanceOf(SystemStatusListener.class));
		this.logger.info("Hello world");
		assertThat(output).contains("Hello world");
	}

	@Test
	void logbackSystemStatusListenerShouldBeRegistered(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, getLogFile(tmpDir() + "/tmp.log", null));
		LoggerContext loggerContext = this.logger.getLoggerContext();
		assertThat(loggerContext.getStatusManager().getCopyOfStatusListenerList()).allSatisfy((listener) -> {
			assertThat(listener).isInstanceOf(SystemStatusListener.class);
			assertThat(listener).hasFieldOrPropertyWithValue("debug", false);
		});
		AlwaysFailAppender appender = new AlwaysFailAppender();
		appender.setContext(loggerContext);
		appender.start();
		this.logger.addAppender(appender);
		this.logger.info("Hello world");
		assertThat(output).contains("Always Fail Appender").contains("Hello world");
	}

	@Test
	void logbackSystemStatusListenerShouldBeRegisteredOnlyOnce() {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, getLogFile(tmpDir() + "/tmp.log", null));
		LoggerContext loggerContext = this.logger.getLoggerContext();
		SystemStatusListener.addTo(loggerContext);
		SystemStatusListener.addTo(loggerContext, true);
		assertThat(loggerContext.getStatusManager().getCopyOfStatusListenerList()).satisfiesOnlyOnce((listener) -> {
			assertThat(listener).isInstanceOf(SystemStatusListener.class);
			assertThat(listener).hasFieldOrPropertyWithValue("debug", false);
		});
	}

	@Test
	void logbackSystemStatusListenerShouldBeRegisteredAndFilterStatusByLevelIfDebugDisabled(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		LoggerContext loggerContext = this.logger.getLoggerContext();
		StatusManager statusManager = loggerContext.getStatusManager();
		statusManager.add(new InfoStatus("INFO STATUS MESSAGE", getClass()));
		statusManager.add(new WarnStatus("WARN STATUS MESSAGE", getClass()));
		statusManager.add(new ErrorStatus("ERROR STATUS MESSAGE", getClass()));
		initialize(this.initializationContext, null, getLogFile(tmpDir() + "/tmp.log", null));
		assertThat(statusManager.getCopyOfStatusListenerList()).allSatisfy((listener) -> {
			assertThat(listener).isInstanceOf(SystemStatusListener.class);
			assertThat(listener).hasFieldOrPropertyWithValue("debug", false);
		});
		this.logger.info("Hello world");
		assertThat(output).doesNotContain("INFO STATUS MESSAGE");
		assertThat(output).contains("WARN STATUS MESSAGE");
		assertThat(output).contains("ERROR STATUS MESSAGE");
		assertThat(output).contains("Hello world");
	}

	@Test
	void logbackSystemStatusListenerShouldBeRegisteredWhenUsingCustomLogbackXml(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, "classpath:logback-include-defaults.xml", null);
		LoggerContext loggerContext = this.logger.getLoggerContext();
		assertThat(loggerContext.getStatusManager().getCopyOfStatusListenerList()).allSatisfy((listener) -> {
			assertThat(listener).isInstanceOf(SystemStatusListener.class);
			assertThat(listener).hasFieldOrPropertyWithValue("debug", false);
		});
		AlwaysFailAppender appender = new AlwaysFailAppender();
		appender.setContext(loggerContext);
		appender.start();
		this.logger.addAppender(appender);
		this.logger.info("Hello world");
		assertThat(output).contains("Always Fail Appender").contains("Hello world");
	}

	@Test
	void testRollingFileNameProperty() {
		String rollingFile = "my.log.%d{yyyyMMdd}.%i.gz";
		this.environment.setProperty("logging.pattern.rolling-file-name", rollingFile);
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "my.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(getRollingPolicy().getFileNamePattern()).isEqualTo(rollingFile);
	}

	@Test
	void customCharset() {
		this.environment.setProperty("logging.charset.console", "UTF-16");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(this.environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		LayoutWrappingEncoder<?> encoder = (LayoutWrappingEncoder<?>) getConsoleAppender().getEncoder();
		assertThat(encoder.getCharset()).isEqualTo(StandardCharsets.UTF_16);
	}

	@Test
	void whenContextHasNoAotContributionThenProcessAheadOfTimeReturnsNull() {
		BeanFactoryInitializationAotContribution contribution = this.loggingSystem.processAheadOfTime(null);
		assertThat(contribution).isNull();
	}

	@Test
	void whenContextHasAotContributionThenProcessAheadOfTimeClearsAndReturnsIt() {
		LoggerContext context = ((LoggerContext) LoggerFactory.getILoggerFactory());
		context.putObject(BeanFactoryInitializationAotContribution.class.getName(),
				mock(BeanFactoryInitializationAotContribution.class));
		BeanFactoryInitializationAotContribution contribution = this.loggingSystem.processAheadOfTime(null);
		assertThat(context.getObject(BeanFactoryInitializationAotContribution.class.getName())).isNull();
		assertThat(contribution).isNotNull();
	}

	@Test // gh-33610
	void springProfileIfNestedWithinSecondPhaseElementSanityChecker(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, "classpath:logback-springprofile-in-root.xml", null);
		this.logger.info("Hello world");
		assertThat(output).contains("<springProfile> elements cannot be nested within an");
	}

	@Test
	void correlationLoggingToFileWhenExpectCorrelationIdTrueAndMdcContent() {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(this.initializationContext, null, logFile);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world"))
			.contains(" [01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void correlationLoggingToConsoleWhenExpectCorrelationIdTrueAndMdcContent(CapturedOutput output) {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
		initialize(this.initializationContext, null, null);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world"))
			.contains(" [01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void correlationLoggingToConsoleWhenExpectCorrelationIdFalseAndMdcContent(CapturedOutput output) {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "false");
		initialize(this.initializationContext, null, null);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).doesNotContain("0123456789012345");
	}

	@Test
	void correlationLoggingToConsoleWhenExpectCorrelationIdTrueAndNoMdcContent(CapturedOutput output) {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
		initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world"))
			.contains(" [                                                 ] ");
	}

	@Test
	void correlationLoggingToConsoleWhenHasCorrelationPattern(CapturedOutput output) {
		this.environment.setProperty("logging.pattern.correlation", "%correlationId{spanId(0),traceId(0)}");
		initialize(this.initializationContext, null, null);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world"))
			.contains(" [0123456789012345-01234567890123456789012345678901] ");
	}

	@Test
	void correlationLoggingToConsoleWhenUsingXmlConfiguration(CapturedOutput output) {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
		initialize(this.initializationContext, "classpath:logback-include-base.xml", null);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world"))
			.contains(" [01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void correlationLoggingToFileWhenUsingFileConfiguration() {
		this.environment.setProperty(LoggingSystem.EXPECT_CORRELATION_ID_PROPERTY, "true");
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(this.initializationContext, "classpath:logback-include-base.xml", logFile);
		MDC.setContextMap(Map.of("traceId", "01234567890123456789012345678901", "spanId", "0123456789012345"));
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world"))
			.contains(" [01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void applicationNameLoggingToConsoleWhenHasApplicationName(CapturedOutput output) {
		this.environment.setProperty("spring.application.name", "myapp");
		initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("[myapp] ");
	}

	@Test
	void applicationNameLoggingToConsoleWhenHasApplicationNameWithParenthesis(CapturedOutput output) {
		this.environment.setProperty("spring.application.name", "myapp (dev)");
		initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("[myapp (dev)] ");
	}

	@Test
	void applicationNameLoggingToConsoleWhenDisabled(CapturedOutput output) {
		this.environment.setProperty("spring.application.name", "myapp");
		this.environment.setProperty("logging.include-application-name", "false");
		initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).doesNotContain("myapp").doesNotContain("null");
	}

	@Test
	void applicationNameLoggingToFileWhenHasApplicationName() {
		this.environment.setProperty("spring.application.name", "myapp");
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("[myapp] ");
	}

	@Test
	void applicationNameLoggingToFileWhenHasApplicationNameWithParenthesis() {
		this.environment.setProperty("spring.application.name", "myapp (dev)");
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("[myapp (dev)] ");
	}

	@Test
	void applicationNameLoggingToFileWhenDisabled() {
		this.environment.setProperty("spring.application.name", "myapp");
		this.environment.setProperty("logging.include-application-name", "false");
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).doesNotContain("myapp").doesNotContain("null");
	}

	@Test
	void whenConfigurationErrorIsDetectedUnderlyingCausesAreIncludedAsSuppressedExceptions() {
		this.loggingSystem.beforeInitialize();
		assertThatIllegalStateException()
			.isThrownBy(() -> initialize(this.initializationContext, "classpath:logback-broken.xml",
					getLogFile(tmpDir() + "/tmp.log", null)))
			.satisfies((ex) -> assertThat(ex.getSuppressed())
				.hasAtLeastOneElementOfType(DynamicClassLoadingException.class));
	}

	@Test
	void whenConfigLocationIsNotXmlThenIllegalArgumentExceptionShouldBeThrown() {
		this.loggingSystem.beforeInitialize();
		assertThatIllegalStateException()
			.isThrownBy(() -> initialize(this.initializationContext, "classpath:logback-invalid-format.txt",
					getLogFile(tmpDir() + "/tmp.log", null)))
			.satisfies((ex) -> assertThat(ex.getCause()).isInstanceOf(JoranException.class)
				.hasMessageStartingWith("Problem parsing XML document. See previously reported errors"));
	}

	@Test
	void whenConfigLocationIsXmlFileWithoutExtensionShouldWork(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, "classpath:logback-without-extension",
				getLogFile(tmpDir() + "/tmp.log", null));
		this.logger.info("No extension and works!");
		assertThat(output.toString()).contains("No extension and works!");
	}

	@Test
	void whenConfigLocationIsXmlAndHasQueryParametersThenIllegalArgumentExceptionShouldNotBeThrown() {
		this.loggingSystem.beforeInitialize();
		assertThatIllegalStateException()
			.isThrownBy(() -> initialize(this.initializationContext, "file:///logback-nonexistent.xml?raw=true",
					getLogFile(tmpDir() + "/tmp.log", null)))
			.satisfies((ex) -> assertThat(ex.getCause()).isNotInstanceOf(IllegalArgumentException.class));
	}

	@Test
	void shouldRespectConsoleThreshold(CapturedOutput output) {
		this.environment.setProperty("logging.threshold.console", "warn");
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		this.logger.info("Some info message");
		this.logger.warn("Some warn message");
		assertThat(output).doesNotContain("Some info message").contains("Some warn message");
	}

	@Test
	void shouldRespectFileThreshold() {
		this.environment.setProperty("logging.threshold.file", "warn");
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
		this.logger.info("Some info message");
		this.logger.warn("Some warn message");
		Path file = Path.of(tmpDir(), "spring.log");
		assertThat(file).content(StandardCharsets.UTF_8)
			.doesNotContain("Some info message")
			.contains("Some warn message");
	}

	@Test
	void applyingSystemPropertiesDoesNotCauseUnwantedStatusWarnings(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		this.environment.getPropertySources()
			.addFirst(new MapPropertySource("test", Map.of("logging.pattern.console", "[CONSOLE]%m")));
		this.loggingSystem.initialize(this.initializationContext, "classpath:logback-nondefault.xml", null);
		assertThat(output).doesNotContain("WARN");
	}

	@Test
	void applicationGroupLoggingToConsoleWhenHasApplicationGroup(CapturedOutput output) {
		this.environment.setProperty("spring.application.group", "mygroup");
		initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("[mygroup] ");
	}

	@Test
	void applicationGroupLoggingToConsoleWhenHasApplicationGroupWithParenthesis(CapturedOutput output) {
		this.environment.setProperty("spring.application.group", "mygroup (dev)");
		initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).contains("[mygroup (dev)] ");
	}

	@Test
	void applicationGroupLoggingToConsoleWhenDisabled(CapturedOutput output) {
		this.environment.setProperty("spring.application.group", "mygroup");
		this.environment.setProperty("logging.include-application-group", "false");
		initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(getLineWithText(output, "Hello world")).doesNotContain("mygroup").doesNotContain("null");
	}

	@Test
	void applicationGroupLoggingToFileWhenHasApplicationGroup() {
		this.environment.setProperty("spring.application.group", "mygroup");
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("[mygroup] ");
	}

	@Test
	void applicationGroupLoggingToFileWhenHasApplicationGroupWithParenthesis() {
		this.environment.setProperty("spring.application.group", "mygroup (dev)");
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("[mygroup (dev)] ");
	}

	@Test
	void applicationGroupLoggingToFileWhenDisabled() {
		this.environment.setProperty("spring.application.group", "myGroup");
		this.environment.setProperty("logging.include-application-group", "false");
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		initialize(this.initializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).doesNotContain("myGroup").doesNotContain("null");
	}

	@Test
	void shouldNotContainAnsiEscapeCodes(CapturedOutput output) {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		assertThat(output).doesNotContain("\033[");
	}

	@Test
	void getEnvironment() {
		this.loggingSystem.beforeInitialize();
		initialize(this.initializationContext, null, null);
		assertThat(this.logger.getLoggerContext().getObject(Environment.class.getName())).isSameAs(this.environment);
	}

	@Test
	void getEnvironmentWhenUsingFile() {
		this.loggingSystem.beforeInitialize();
		LogFile logFile = getLogFile(tmpDir() + "/example.log", null, false);
		initialize(this.initializationContext, "classpath:logback-nondefault.xml", logFile);
		assertThat(this.logger.getLoggerContext().getObject(Environment.class.getName())).isSameAs(this.environment);
	}

	private void initialize(LoggingInitializationContext context, String configLocation, LogFile logFile) {
		this.loggingSystem.getSystemProperties((ConfigurableEnvironment) context.getEnvironment()).apply(logFile);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(context, configLocation, logFile);
	}

	private static Logger getRootLogger() {
		ILoggerFactory factory = LoggerFactory.getILoggerFactory();
		LoggerContext context = (LoggerContext) factory;
		return context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
	}

	private static ConsoleAppender<?> getConsoleAppender() {
		return (ConsoleAppender<?>) getRootLogger().getAppender("CONSOLE");
	}

	private static RollingFileAppender<?> getFileAppender() {
		return (RollingFileAppender<?>) getRootLogger().getAppender("FILE");
	}

	private static SizeAndTimeBasedRollingPolicy<?> getRollingPolicy() {
		return (SizeAndTimeBasedRollingPolicy<?>) getFileAppender().getRollingPolicy();
	}

	private static final class AlwaysFailAppender extends AppenderBase<ILoggingEvent> {

		@Override
		protected void append(ILoggingEvent eventObject) {
			throw new RuntimeException("Always Fail Appender");
		}

	}

}
