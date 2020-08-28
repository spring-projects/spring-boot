/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.context.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.impl.StaticLoggerBinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.logging.AbstractLoggingSystem;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggerConfiguration;
import org.springframework.boot.logging.LoggerGroups;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.boot.logging.java.JavaLoggingSystem;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link LoggingApplicationListener} with Logback.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Ben Hale
 * @author Fahim Farook
 * @author Eddú Meléndez
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathExclusions("log4j*.jar")
class LoggingApplicationListenerTests {

	private static final String[] NO_ARGS = {};

	private final LoggingApplicationListener initializer = new LoggingApplicationListener();

	private final LoggerContext loggerContext = (LoggerContext) StaticLoggerBinder.getSingleton().getLoggerFactory();

	private final ch.qos.logback.classic.Logger logger = this.loggerContext.getLogger(getClass());

	private final SpringApplication springApplication = new SpringApplication();

	private final GenericApplicationContext context = new GenericApplicationContext();

	@TempDir
	public Path tempDir;

	private File logFile;

	private CapturedOutput output;

	@BeforeEach
	void init(CapturedOutput output) throws SecurityException, IOException {
		this.output = output;
		this.logFile = new File(this.tempDir.toFile(), "foo.log");
		LogManager.getLogManager().readConfiguration(JavaLoggingSystem.class.getResourceAsStream("logging.properties"));
		multicastEvent(new ApplicationStartingEvent(new SpringApplication(), NO_ARGS));
		new File(this.tempDir.toFile(), "spring.log").delete();
		ConfigurableEnvironment environment = this.context.getEnvironment();
		ConfigurationPropertySources.attach(environment);
	}

	@AfterEach
	void clear() throws IOException {
		LoggingSystem loggingSystem = LoggingSystem.get(getClass().getClassLoader());
		loggingSystem.setLogLevel("ROOT", LogLevel.INFO);
		loggingSystem.cleanUp();
		if (loggingSystem.getShutdownHandler() != null) {
			loggingSystem.getShutdownHandler().run();
		}
		System.clearProperty(LoggingSystem.class.getName());
		System.clearProperty(LoggingSystemProperties.LOG_FILE);
		System.clearProperty(LoggingSystemProperties.LOG_PATH);
		System.clearProperty(LoggingSystemProperties.PID_KEY);
		System.clearProperty(LoggingSystemProperties.EXCEPTION_CONVERSION_WORD);
		System.clearProperty(LoggingSystemProperties.CONSOLE_LOG_PATTERN);
		System.clearProperty(LoggingSystemProperties.FILE_LOG_PATTERN);
		System.clearProperty(LoggingSystemProperties.LOG_LEVEL_PATTERN);
		System.clearProperty(LoggingSystemProperties.ROLLING_FILE_NAME_PATTERN);
		System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void baseConfigLocation() {
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.info("Hello world", new RuntimeException("Expected"));
		assertThat(this.output).contains("Hello world");
		assertThat(this.output).doesNotContain("???");
		assertThat(this.output).contains("[junit-");
		assertThat(new File(this.tempDir + "/spring.log").exists()).isFalse();
	}

	@Test
	void overrideConfigLocation() {
		addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-nondefault.xml");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.info("Hello world");
		assertThat(this.output).contains("Hello world").doesNotContain("???").startsWith("null ").endsWith("BOOTBOOT");
	}

	@Test
	void overrideConfigDoesNotExist() {
		addPropertiesToEnvironment(this.context, "logging.config=doesnotexist.xml");
		assertThatIllegalStateException().isThrownBy(
				() -> this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader()));
		assertThat(this.output)
				.contains("Logging system failed to initialize using configuration from 'doesnotexist.xml'")
				.doesNotContain("JoranException");
	}

	@Test
	void azureDefaultLoggingConfigDoesNotCauseAFailure() {
		addPropertiesToEnvironment(this.context,
				"logging.config=-Djava.util.logging.config.file=\"d:\\home\\site\\wwwroot\\bin\\apache-tomcat-7.0.52\\conf\\logging.properties\"");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.info("Hello world");
		assertThat(this.output).contains("Hello world").doesNotContain("???");
		assertThat(new File(this.tempDir.toFile(), "/spring.log").exists()).isFalse();
	}

	@Test
	void tomcatNopLoggingConfigDoesNotCauseAFailure() {
		addPropertiesToEnvironment(this.context, "LOGGING_CONFIG=-Dnop");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.info("Hello world");
		assertThat(this.output).contains("Hello world").doesNotContain("???");
		assertThat(new File(this.tempDir.toFile(), "/spring.log").exists()).isFalse();
	}

	@Test
	void overrideConfigBroken() {
		addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-broken.xml");
		assertThatIllegalStateException().isThrownBy(() -> {
			this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
			assertThat(this.output).contains(
					"Logging system failed to initialize using configuration from 'classpath:logback-broken.xml'");
			assertThat(this.output).contains("ConsolAppender");
		});
	}

	@Test
	void addLogFileProperty() {
		addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-nondefault.xml",
				"logging.file.name=" + this.logFile);
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		String existingOutput = this.output.toString();
		logger.info("Hello world");
		String output = this.output.toString().substring(existingOutput.length()).trim();
		assertThat(output).startsWith(this.logFile.getAbsolutePath());
	}

	@Test
	void addLogFilePropertyWithDefault() {
		assertThat(this.logFile).doesNotExist();
		addPropertiesToEnvironment(this.context, "logging.file.name=" + this.logFile);
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		logger.info("Hello world");
		assertThat(this.logFile).isFile();
	}

	@Test
	void addLogPathProperty() {
		addPropertiesToEnvironment(this.context, "logging.config=classpath:logback-nondefault.xml",
				"logging.file.path=" + this.tempDir);
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		String existingOutput = this.output.toString();
		logger.info("Hello world");
		String output = this.output.toString().substring(existingOutput.length()).trim();
		assertThat(output).startsWith(new File(this.tempDir.toFile(), "spring.log").getAbsolutePath());
	}

	@Test
	void parseDebugArg() {
		addPropertiesToEnvironment(this.context, "debug");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.output).contains("testatdebug");
		assertThat(this.output).doesNotContain("testattrace");
	}

	@Test
	void parseDebugArgExpandGroups() {
		addPropertiesToEnvironment(this.context, "debug");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.loggerContext.getLogger("org.springframework.boot.actuate.endpoint.web").debug("testdebugwebgroup");
		this.loggerContext.getLogger("org.hibernate.SQL").debug("testdebugsqlgroup");
		assertThat(this.output).contains("testdebugwebgroup");
		assertThat(this.output).contains("testdebugsqlgroup");
		LoggerGroups loggerGroups = (LoggerGroups) ReflectionTestUtils.getField(this.initializer, "loggerGroups");
		assertThat(loggerGroups.get("web").getConfiguredLevel()).isEqualTo(LogLevel.DEBUG);
	}

	@Test
	void parseTraceArg() {
		addPropertiesToEnvironment(this.context, "trace");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.output).contains("testatdebug");
		assertThat(this.output).contains("testattrace");
	}

	@Test
	void disableDebugArg() {
		disableDebugTraceArg("debug=false");
	}

	@Test
	void disableTraceArg() {
		disableDebugTraceArg("trace=false");
	}

	private void disableDebugTraceArg(String... environment) {
		addPropertiesToEnvironment(this.context, environment);
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.output).doesNotContain("testatdebug");
		assertThat(this.output).doesNotContain("testattrace");
	}

	@Test
	void parseLevels() {
		addPropertiesToEnvironment(this.context, "logging.level.org.springframework.boot=TRACE");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.output).contains("testatdebug");
		assertThat(this.output).contains("testattrace");
	}

	@Test
	void parseLevelsCaseInsensitive() {
		addPropertiesToEnvironment(this.context, "logging.level.org.springframework.boot=TrAcE");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.output).contains("testatdebug");
		assertThat(this.output).contains("testattrace");
	}

	@Test
	void parseLevelsTrimsWhitespace() {
		addPropertiesToEnvironment(this.context, "logging.level.org.springframework.boot= trace ");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.output).contains("testatdebug");
		assertThat(this.output).contains("testattrace");
	}

	@Test
	void parseLevelsWithPlaceholder() {
		addPropertiesToEnvironment(this.context, "foo=TRACE", "logging.level.org.springframework.boot=${foo}");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.output).contains("testatdebug");
		assertThat(this.output).contains("testattrace");
	}

	@Test
	void parseLevelsFails() {
		this.logger.setLevel(Level.INFO);
		addPropertiesToEnvironment(this.context, "logging.level.org.springframework.boot=GARBAGE");
		assertThatExceptionOfType(BindException.class).isThrownBy(
				() -> this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader()));
	}

	@Test
	void parseLevelsNone() {
		addPropertiesToEnvironment(this.context, "logging.level.org.springframework.boot=OFF");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.error("testaterror");
		assertThat(this.output).doesNotContain("testatdebug").doesNotContain("testaterror");
	}

	@Test
	void parseLevelsMapsFalseToOff() {
		addPropertiesToEnvironment(this.context, "logging.level.org.springframework.boot=false");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.error("testaterror");
		assertThat(this.output).doesNotContain("testatdebug").doesNotContain("testaterror");
	}

	@Test
	void parseArgsDisabled() {
		this.initializer.setParseArgs(false);
		addPropertiesToEnvironment(this.context, "debug");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.output).doesNotContain("testatdebug");
	}

	@Test
	void parseArgsDoesntReplace() {
		this.initializer.setSpringBootLogging(LogLevel.ERROR);
		this.initializer.setParseArgs(false);
		multicastEvent(new ApplicationStartingEvent(this.springApplication, new String[] { "--debug" }));
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.output).doesNotContain("testatdebug");
	}

	@Test
	void bridgeHandlerLifecycle() {
		assertThat(bridgeHandlerInstalled()).isTrue();
		multicastEvent(new ContextClosedEvent(this.context));
		assertThat(bridgeHandlerInstalled()).isFalse();
	}

	@Test
	void defaultExceptionConversionWord() {
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.info("Hello world", new RuntimeException("Wrapper", new RuntimeException("Expected")));
		assertThat(this.output).contains("Hello world");
		assertThat(this.output).doesNotContain("Wrapped by: java.lang.RuntimeException: Wrapper");
	}

	@Test
	void overrideExceptionConversionWord() {
		addPropertiesToEnvironment(this.context, "logging.exceptionConversionWord=%rEx");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.info("Hello world", new RuntimeException("Wrapper", new RuntimeException("Expected")));
		assertThat(this.output).contains("Hello world");
		assertThat(this.output).contains("Wrapped by: java.lang.RuntimeException: Wrapper");
	}

	@Test
	void shutdownHookIsNotRegisteredByDefault() {
		TestLoggingApplicationListener listener = new TestLoggingApplicationListener();
		System.setProperty(LoggingSystem.class.getName(), TestShutdownHandlerLoggingSystem.class.getName());
		multicastEvent(listener, new ApplicationStartingEvent(new SpringApplication(), NO_ARGS));
		listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertThat(listener.shutdownHook).isNull();
	}

	@Test
	void shutdownHookCanBeRegistered() throws Exception {
		TestLoggingApplicationListener listener = new TestLoggingApplicationListener();
		System.setProperty(LoggingSystem.class.getName(), TestShutdownHandlerLoggingSystem.class.getName());
		addPropertiesToEnvironment(this.context, "logging.register_shutdown_hook=true");
		multicastEvent(listener, new ApplicationStartingEvent(new SpringApplication(), NO_ARGS));
		listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertThat(listener.shutdownHook).isNotNull();
		listener.shutdownHook.start();
		assertThat(TestShutdownHandlerLoggingSystem.shutdownLatch.await(30, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void closingContextCleansUpLoggingSystem() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, TestCleanupLoggingSystem.class.getName());
		multicastEvent(new ApplicationStartingEvent(this.springApplication, new String[0]));
		TestCleanupLoggingSystem loggingSystem = (TestCleanupLoggingSystem) ReflectionTestUtils
				.getField(this.initializer, "loggingSystem");
		assertThat(loggingSystem.cleanedUp).isFalse();
		multicastEvent(new ContextClosedEvent(this.context));
		assertThat(loggingSystem.cleanedUp).isTrue();
	}

	@Test
	void closingChildContextDoesNotCleanUpLoggingSystem() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, TestCleanupLoggingSystem.class.getName());
		multicastEvent(new ApplicationStartingEvent(this.springApplication, new String[0]));
		TestCleanupLoggingSystem loggingSystem = (TestCleanupLoggingSystem) ReflectionTestUtils
				.getField(this.initializer, "loggingSystem");
		assertThat(loggingSystem.cleanedUp).isFalse();
		GenericApplicationContext childContext = new GenericApplicationContext();
		childContext.setParent(this.context);
		multicastEvent(new ContextClosedEvent(childContext));
		assertThat(loggingSystem.cleanedUp).isFalse();
		multicastEvent(new ContextClosedEvent(this.context));
		assertThat(loggingSystem.cleanedUp).isTrue();
		childContext.close();
	}

	@Test
	void systemPropertiesAreSetForLoggingConfiguration() {
		addPropertiesToEnvironment(this.context, "logging.exception-conversion-word=conversion",
				"logging.file.name=" + this.logFile, "logging.file.path=path", "logging.pattern.console=console",
				"logging.pattern.file=file", "logging.pattern.level=level",
				"logging.pattern.rolling-file-name=my.log.%d{yyyyMMdd}.%i.gz");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertThat(System.getProperty(LoggingSystemProperties.CONSOLE_LOG_PATTERN)).isEqualTo("console");
		assertThat(System.getProperty(LoggingSystemProperties.FILE_LOG_PATTERN)).isEqualTo("file");
		assertThat(System.getProperty(LoggingSystemProperties.EXCEPTION_CONVERSION_WORD)).isEqualTo("conversion");
		assertThat(System.getProperty(LoggingSystemProperties.LOG_FILE)).isEqualTo(this.logFile.getAbsolutePath());
		assertThat(System.getProperty(LoggingSystemProperties.LOG_LEVEL_PATTERN)).isEqualTo("level");
		assertThat(System.getProperty(LoggingSystemProperties.LOG_PATH)).isEqualTo("path");
		assertThat(System.getProperty(LoggingSystemProperties.ROLLING_FILE_NAME_PATTERN))
				.isEqualTo("my.log.%d{yyyyMMdd}.%i.gz");
		assertThat(System.getProperty(LoggingSystemProperties.PID_KEY)).isNotNull();
	}

	@Test
	void environmentPropertiesIgnoreUnresolvablePlaceholders() {
		// gh-7719
		addPropertiesToEnvironment(this.context, "logging.pattern.console=console ${doesnotexist}");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertThat(System.getProperty(LoggingSystemProperties.CONSOLE_LOG_PATTERN))
				.isEqualTo("console ${doesnotexist}");
	}

	@Test
	void environmentPropertiesResolvePlaceholders() {
		addPropertiesToEnvironment(this.context, "logging.pattern.console=console ${pid}");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertThat(System.getProperty(LoggingSystemProperties.CONSOLE_LOG_PATTERN))
				.isEqualTo(this.context.getEnvironment().getProperty("logging.pattern.console"));
	}

	@Test
	void logFilePropertiesCanReferenceSystemProperties() {
		addPropertiesToEnvironment(this.context, "logging.file.name=" + this.tempDir + "${PID}.log");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertThat(System.getProperty(LoggingSystemProperties.LOG_FILE))
				.isEqualTo(this.tempDir + new ApplicationPid().toString() + ".log");
	}

	@Test
	void applicationFailedEventCleansUpLoggingSystem() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY, TestCleanupLoggingSystem.class.getName());
		multicastEvent(new ApplicationStartingEvent(this.springApplication, new String[0]));
		TestCleanupLoggingSystem loggingSystem = (TestCleanupLoggingSystem) ReflectionTestUtils
				.getField(this.initializer, "loggingSystem");
		assertThat(loggingSystem.cleanedUp).isFalse();
		multicastEvent(new ApplicationFailedEvent(this.springApplication, new String[0],
				new GenericApplicationContext(), new Exception()));
		assertThat(loggingSystem.cleanedUp).isTrue();
	}

	@Test
	void lowPriorityPropertySourceShouldNotOverrideRootLoggerConfig() {
		MutablePropertySources propertySources = this.context.getEnvironment().getPropertySources();
		propertySources
				.addFirst(new MapPropertySource("test1", Collections.singletonMap("logging.level.ROOT", "DEBUG")));
		propertySources.addLast(new MapPropertySource("test2", Collections.singletonMap("logging.level.root", "WARN")));
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.output).contains("testatdebug");
	}

	@Test
	void loggingGroupsDefaultsAreApplied() {
		addPropertiesToEnvironment(this.context, "logging.level.web=TRACE");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertTraceEnabled("org.springframework.core", false);
		assertTraceEnabled("org.springframework.core.codec", true);
		assertTraceEnabled("org.springframework.http", true);
		assertTraceEnabled("org.springframework.web", true);
		assertTraceEnabled("org.springframework.boot.actuate.endpoint.web", true);
		assertTraceEnabled("org.springframework.boot.web.servlet.ServletContextInitializerBeans", true);
	}

	@Test
	void loggingGroupsCanBeDefined() {
		addPropertiesToEnvironment(this.context, "logging.group.foo=com.foo.bar,com.foo.baz",
				"logging.level.foo=TRACE");
		this.initializer.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertTraceEnabled("com.foo", false);
		assertTraceEnabled("com.foo.bar", true);
		assertTraceEnabled("com.foo.baz", true);
	}

	private void assertTraceEnabled(String name, boolean expected) {
		assertThat(this.loggerContext.getLogger(name).isTraceEnabled()).isEqualTo(expected);
	}

	private void multicastEvent(ApplicationEvent event) {
		multicastEvent(this.initializer, event);
	}

	private void multicastEvent(ApplicationListener<?> listener, ApplicationEvent event) {
		SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
		multicaster.addApplicationListener(listener);
		multicaster.multicastEvent(event);
	}

	private boolean bridgeHandlerInstalled() {
		Logger rootLogger = LogManager.getLogManager().getLogger("");
		Handler[] handlers = rootLogger.getHandlers();
		for (Handler handler : handlers) {
			if (handler instanceof SLF4JBridgeHandler) {
				return true;
			}
		}
		return false;
	}

	private void addPropertiesToEnvironment(ConfigurableApplicationContext context, String... pairs) {
		ConfigurableEnvironment environment = context.getEnvironment();
		Map<String, Object> properties = new HashMap<>();
		for (String pair : pairs) {
			String[] split = pair.split("=", 2);
			properties.put(split[0], (split.length == 2) ? split[1] : "");
		}
		MapPropertySource propertySource = new MapPropertySource("logging-config", properties);
		environment.getPropertySources().addFirst(propertySource);
	}

	static class TestShutdownHandlerLoggingSystem extends AbstractLoggingSystem {

		private static CountDownLatch shutdownLatch;

		TestShutdownHandlerLoggingSystem(ClassLoader classLoader) {
			super(classLoader);
			TestShutdownHandlerLoggingSystem.shutdownLatch = new CountDownLatch(1);
		}

		@Override
		protected String[] getStandardConfigLocations() {
			return new String[] { "foo.bar" };
		}

		@Override
		protected void loadDefaults(LoggingInitializationContext initializationContext, LogFile logFile) {
		}

		@Override
		protected void loadConfiguration(LoggingInitializationContext initializationContext, String location,
				LogFile logFile) {
		}

		@Override
		public void setLogLevel(String loggerName, LogLevel level) {
		}

		@Override
		public List<LoggerConfiguration> getLoggerConfigurations() {
			return null;
		}

		@Override
		public LoggerConfiguration getLoggerConfiguration(String loggerName) {
			return null;
		}

		@Override
		public Runnable getShutdownHandler() {
			return () -> TestShutdownHandlerLoggingSystem.shutdownLatch.countDown();
		}

	}

	static class TestLoggingApplicationListener extends LoggingApplicationListener {

		private Thread shutdownHook;

		@Override
		void registerShutdownHook(Thread shutdownHook) {
			this.shutdownHook = shutdownHook;
		}

	}

	static final class TestCleanupLoggingSystem extends LoggingSystem {

		private boolean cleanedUp = false;

		TestCleanupLoggingSystem(ClassLoader classLoader) {
		}

		@Override
		public void beforeInitialize() {
		}

		@Override
		public void setLogLevel(String loggerName, LogLevel level) {
		}

		@Override
		public List<LoggerConfiguration> getLoggerConfigurations() {
			return null;
		}

		@Override
		public LoggerConfiguration getLoggerConfiguration(String loggerName) {
			return null;
		}

		@Override
		public void cleanUp() {
			this.cleanedUp = true;
		}

	}

}
