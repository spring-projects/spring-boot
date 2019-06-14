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

package org.springframework.boot.logging.logback;

import java.io.File;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogManager;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.ILoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.impl.StaticLoggerBinder;

import org.springframework.boot.logging.AbstractLoggingSystemTests;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.boot.testsupport.assertj.Matched;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.boot.testsupport.system.OutputCaptureRule;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.contentOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("log4j-*.jar")
public class LogbackLoggingSystemTests extends AbstractLoggingSystemTests {

	@Rule
	public OutputCaptureRule output = new OutputCaptureRule();

	private final LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(getClass().getClassLoader());

	private Logger logger;

	private LoggingInitializationContext initializationContext;

	@Before
	public void setup() {
		this.loggingSystem.cleanUp();
		this.logger = ((LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory()).getLogger(getClass());
		MockEnvironment environment = new MockEnvironment();
		this.initializationContext = new LoggingInitializationContext(environment);
	}

	@Override
	@After
	public void clear() {
		super.clear();
		this.loggingSystem.cleanUp();
		((LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory()).stop();
	}

	@Test
	public void noFile() {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(getLineWithText(output, "Hello world")).contains("INFO");
		assertThat(new File(tmpDir() + "/spring.log").exists()).isFalse();
	}

	@Test
	public void withFile() {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		File file = new File(tmpDir() + "/spring.log");
		assertThat(output).contains("Hello world").doesNotContain("Hidden");
		assertThat(getLineWithText(output, "Hello world")).contains("INFO");
		assertThat(file.exists()).isTrue();
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "maxFileSize").toString()).isEqualTo("10 MB");
		assertThat(getRollingPolicy().getMaxHistory()).isEqualTo(7);
	}

	@Test
	public void defaultConfigConfiguresAConsoleAppender() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		assertThat(getConsoleAppender()).isNotNull();
	}

	@Test
	public void testNonDefaultConfigLocation() {
		int existingOutputLength = this.output.toString().length();
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, "classpath:logback-nondefault.xml",
				getLogFile(tmpDir() + "/tmp.log", null));
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertThat(output.substring(existingOutputLength)).doesNotContain("DEBUG");
		assertThat(output).contains("Hello world").contains(tmpDir() + "/tmp.log");
		assertThat(output).endsWith("BOOTBOOT");
		assertThat(new File(tmpDir() + "/tmp.log").exists()).isFalse();
	}

	@Test
	public void testLogbackSpecificSystemProperty() {
		System.setProperty("logback.configurationFile", "/foo/my-file.xml");
		try {
			this.loggingSystem.beforeInitialize();
			this.loggingSystem.initialize(this.initializationContext, null, null);
			String output = this.output.toString().trim();
			assertThat(output).contains(
					"Ignoring 'logback.configurationFile' " + "system property. Please use 'logging.config' instead.");
		}
		finally {
			System.clearProperty("logback.configurationFile");
		}
	}

	@Test
	public void testNonexistentConfigLocation() {
		this.loggingSystem.beforeInitialize();
		assertThatIllegalStateException().isThrownBy(() -> this.loggingSystem.initialize(this.initializationContext,
				"classpath:logback-nonexistent.xml", null));
	}

	@Test
	public void getSupportedLevels() {
		assertThat(this.loggingSystem.getSupportedLogLevels()).isEqualTo(
				EnumSet.of(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.OFF));
	}

	@Test
	public void setLevel() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.debug("Hello");
		assertThat(StringUtils.countOccurrencesOf(this.output.toString(), "Hello")).isEqualTo(1);
	}

	@Test
	public void setLevelToNull() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", null);
		this.logger.debug("Hello");
		assertThat(StringUtils.countOccurrencesOf(this.output.toString(), "Hello")).isEqualTo(1);
	}

	@Test
	public void getLoggingConfigurations() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		List<LoggerConfiguration> configurations = this.loggingSystem.getLoggerConfigurations();
		assertThat(configurations).isNotEmpty();
		assertThat(configurations.get(0).getName()).isEqualTo(LoggingSystem.ROOT_LOGGER_NAME);
	}

	@Test
	public void getLoggingConfiguration() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(getClass().getName());
		assertThat(configuration)
				.isEqualTo(new LoggerConfiguration(getClass().getName(), LogLevel.DEBUG, LogLevel.DEBUG));
	}

	@Test
	public void getLoggingConfigurationForALL() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		Logger logger = (Logger) StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger(getClass().getName());
		logger.setLevel(Level.ALL);
		LoggerConfiguration configuration = this.loggingSystem.getLoggerConfiguration(getClass().getName());
		assertThat(configuration)
				.isEqualTo(new LoggerConfiguration(getClass().getName(), LogLevel.TRACE, LogLevel.TRACE));
	}

	@Test
	public void systemLevelTraceShouldReturnNativeLevelTraceNotAll() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.TRACE);
		Logger logger = (Logger) StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger(getClass().getName());
		assertThat(logger.getLevel()).isEqualTo(Level.TRACE);
	}

	@Test
	public void loggingThatUsesJulIsCaptured() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(getClass().getName());
		julLogger.info("Hello world");
		String output = this.output.toString().trim();
		assertThat(output).contains("Hello world");
	}

	@Test
	public void loggingLevelIsPropagatedToJul() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.setLogLevel(getClass().getName(), LogLevel.DEBUG);
		java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(getClass().getName());
		julLogger.fine("Hello debug world");
		String output = this.output.toString().trim();
		assertThat(output).contains("Hello debug world");
	}

	@Test
	public void bridgeHandlerLifecycle() {
		assertThat(bridgeHandlerInstalled()).isFalse();
		this.loggingSystem.beforeInitialize();
		assertThat(bridgeHandlerInstalled()).isTrue();
		this.loggingSystem.cleanUp();
		assertThat(bridgeHandlerInstalled()).isFalse();
	}

	@Test
	public void standardConfigLocations() {
		String[] locations = this.loggingSystem.getStandardConfigLocations();
		assertThat(locations).containsExactly("logback-test.groovy", "logback-test.xml", "logback.groovy",
				"logback.xml");
	}

	@Test
	public void springConfigLocations() {
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
	public void testConsolePatternProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.pattern.console", "%logger %msg");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		this.loggingSystem.initialize(loggingInitializationContext, null, null);
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertThat(getLineWithText(output, "Hello world")).doesNotContain("INFO");
	}

	@Test
	public void testLevelPatternProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.pattern.level", "X%clr(%p)X");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		this.loggingSystem.initialize(loggingInitializationContext, null, null);
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertThat(getLineWithText(output, "Hello world")).contains("XINFOX");
	}

	@Test
	public void testFilePatternProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.pattern.file", "%logger %msg");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertThat(getLineWithText(output, "Hello world")).contains("INFO");
		assertThat(getLineWithText(file, "Hello world")).doesNotContain("INFO");
	}

	@Test
	public void testCleanHistoryOnStartProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.file.clean-history-on-start", "true");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(getRollingPolicy().isCleanHistoryOnStart()).isTrue();
	}

	@Test
	public void testCleanHistoryOnStartPropertyWithXmlConfiguration() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.file.clean-history-on-start", "true");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(loggingInitializationContext, "classpath:logback-include-base.xml", logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(getRollingPolicy().isCleanHistoryOnStart()).isTrue();
	}

	@Test
	public void testMaxFileSizePropertyWithLogbackFileSize() {
		testMaxFileSizeProperty("100 MB", "100 MB");
	}

	@Test
	public void testMaxFileSizePropertyWithDataSize() {
		testMaxFileSizeProperty("15MB", "15 MB");
	}

	@Test
	public void testMaxFileSizePropertyWithBytesValue() {
		testMaxFileSizeProperty(String.valueOf(10 * 1024 * 1024), "10 MB");
	}

	private void testMaxFileSizeProperty(String sizeValue, String expectedFileSize) {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.file.max-size", sizeValue);
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "maxFileSize").toString())
				.isEqualTo(expectedFileSize);
	}

	@Test
	public void testMaxFileSizePropertyWithXmlConfiguration() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.file.max-size", "100MB");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(loggingInitializationContext, "classpath:logback-include-base.xml", logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "maxFileSize").toString()).isEqualTo("100 MB");
	}

	@Test
	public void testMaxHistoryProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.file.max-history", "30");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(getRollingPolicy().getMaxHistory()).isEqualTo(30);
	}

	@Test
	public void testMaxHistoryPropertyWithXmlConfiguration() throws Exception {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.file.max-history", "30");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(loggingInitializationContext, "classpath:logback-include-base.xml", logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(getRollingPolicy().getMaxHistory()).isEqualTo(30);
	}

	@Test
	public void testTotalSizeCapPropertyWithLogbackFileSize() {
		testTotalSizeCapProperty("101 MB", "101 MB");
	}

	@Test
	public void testTotalSizeCapPropertyWithDataSize() {
		testTotalSizeCapProperty("10MB", "10 MB");
	}

	@Test
	public void testTotalSizeCapPropertyWithBytesValue() {
		testTotalSizeCapProperty(String.valueOf(10 * 1024 * 1024), "10 MB");
	}

	private void testTotalSizeCapProperty(String sizeValue, String expectedFileSize) {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.file.total-size-cap", sizeValue);
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(loggingInitializationContext, null, logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "totalSizeCap").toString())
				.isEqualTo(expectedFileSize);
	}

	@Test
	public void testTotalSizeCapPropertyWithXmlConfiguration() {
		String expectedSize = "101 MB";
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.file.total-size-cap", expectedSize);
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(loggingInitializationContext, "classpath:logback-include-base.xml", logFile);
		this.logger.info("Hello world");
		assertThat(getLineWithText(file, "Hello world")).contains("INFO");
		assertThat(ReflectionTestUtils.getField(getRollingPolicy(), "totalSizeCap").toString()).isEqualTo(expectedSize);
	}

	@Test
	public void exceptionsIncludeClassPackaging() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
		Matcher<String> expectedOutput = containsString("[junit-");
		this.output.expect(expectedOutput);
		this.logger.warn("Expected exception", new RuntimeException("Expected"));
		String fileContents = contentOf(new File(tmpDir() + "/spring.log"));
		assertThat(fileContents).is(Matched.by(expectedOutput));
	}

	@Test
	public void customExceptionConversionWord() {
		System.setProperty(LoggingSystemProperties.EXCEPTION_CONVERSION_WORD, "%ex");
		try {
			this.loggingSystem.beforeInitialize();
			this.logger.info("Hidden");
			this.loggingSystem.initialize(this.initializationContext, null, getLogFile(null, tmpDir()));
			Matcher<String> expectedOutput = Matchers.allOf(containsString("java.lang.RuntimeException: Expected"),
					not(containsString("Wrapped by:")));
			this.output.expect(expectedOutput);
			this.logger.warn("Expected exception", new RuntimeException("Expected", new RuntimeException("Cause")));
			String fileContents = contentOf(new File(tmpDir() + "/spring.log"));
			assertThat(fileContents).is(Matched.by(expectedOutput));
		}
		finally {
			System.clearProperty(LoggingSystemProperties.EXCEPTION_CONVERSION_WORD);
		}
	}

	@Test
	public void initializeShouldSetSystemProperty() {
		// gh-5491
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		LogFile logFile = getLogFile(tmpDir() + "/example.log", null, false);
		this.loggingSystem.initialize(this.initializationContext, "classpath:logback-nondefault.xml", logFile);
		assertThat(System.getProperty(LoggingSystemProperties.LOG_FILE)).endsWith("example.log");
	}

	@Test
	public void initializationIsOnlyPerformedOnceUntilCleanedUp() {
		LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();
		LoggerContextListener listener = mock(LoggerContextListener.class);
		loggerContext.addListener(listener);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		verify(listener, times(1)).onReset(loggerContext);
		this.loggingSystem.cleanUp();
		loggerContext.addListener(listener);
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(this.initializationContext, null, null);
		verify(listener, times(2)).onReset(loggerContext);
	}

	@Test
	public void testDateformatPatternProperty() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.pattern.dateformat", "yyyy-MM-dd'T'hh:mm:ss.SSSZ");
		LoggingInitializationContext loggingInitializationContext = new LoggingInitializationContext(environment);
		this.loggingSystem.initialize(loggingInitializationContext, null, null);
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertThat(getLineWithText(output, "Hello world"))
				.containsPattern("\\d{4}-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2}");
	}

	@Test
	public void noDebugOutputIsProducedByDefault() {
		System.clearProperty("logback.debug");
		this.loggingSystem.beforeInitialize();
		File file = new File(tmpDir(), "logback-test.log");
		LogFile logFile = getLogFile(file.getPath(), null);
		this.loggingSystem.initialize(this.initializationContext, null, logFile);
		String output = this.output.toString().trim();
		System.out.println(output);
		assertThat(output).doesNotContain("LevelChangePropagator").doesNotContain("SizeAndTimeBasedFNATP");
	}

	@Test
	public void logbackDebugPropertyIsHonored() {
		System.setProperty("logback.debug", "true");
		try {
			this.loggingSystem.beforeInitialize();
			File file = new File(tmpDir(), "logback-test.log");
			LogFile logFile = getLogFile(file.getPath(), null);
			this.loggingSystem.initialize(this.initializationContext, null, logFile);
			String output = this.output.toString().trim();
			assertThat(output).contains("LevelChangePropagator").contains("SizeAndTimeBasedFNATP")
					.contains("DebugLogbackConfigurator");
		}
		finally {
			System.clearProperty("logback.debug");
		}
	}

	private static Logger getRootLogger() {
		ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
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

	private String getLineWithText(File file, String outputSearch) {
		return getLineWithText(contentOf(file), outputSearch);
	}

	private String getLineWithText(String output, String outputSearch) {
		String[] lines = output.split("\\r?\\n");
		for (String line : lines) {
			if (line.contains(outputSearch)) {
				return line;
			}
		}
		return null;
	}

}
