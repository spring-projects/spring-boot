/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.logging;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SLF4JLogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.bridge.SLF4JBridgeHandler;

import org.springframework.boot.ApplicationPid;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.logging.java.JavaLoggingSystem;
import org.springframework.boot.testutil.InternalOutputCapture;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Tests for {@link LoggingApplicationListener}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class LoggingApplicationListenerTests {

	private static final String[] NO_ARGS = {};

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public InternalOutputCapture outputCapture = new InternalOutputCapture();

	private final LoggingApplicationListener initializer = new LoggingApplicationListener();

	private final Log logger = new SLF4JLogFactory().getInstance(getClass());

	private final SpringApplication springApplication = new SpringApplication();

	private final GenericApplicationContext context = new GenericApplicationContext();

	@Before
	public void init() throws SecurityException, IOException {
		LogManager.getLogManager().readConfiguration(
				JavaLoggingSystem.class.getResourceAsStream("logging.properties"));
		this.initializer.onApplicationEvent(
				new ApplicationStartedEvent(new SpringApplication(), NO_ARGS));
		new File("target/foo.log").delete();
		new File(tmpDir() + "/spring.log").delete();
	}

	@After
	public void clear() {
		LoggingSystem.get(getClass().getClassLoader()).cleanUp();
		System.clearProperty(LoggingSystem.class.getName());
		System.clearProperty("LOG_FILE");
		System.clearProperty("LOG_PATH");
		System.clearProperty("PID");
		System.clearProperty("LOG_EXCEPTION_CONVERSION_WORD");
		System.clearProperty("CONSOLE_LOG_PATTERN");
		System.clearProperty("FILE_LOG_PATTERN");
		System.clearProperty("LOG_LEVEL_PATTERN");
		System.clearProperty(LoggingSystem.SYSTEM_PROPERTY);
		if (this.context != null) {
			this.context.close();
		}
	}

	private String tmpDir() {
		String path = this.context.getEnvironment()
				.resolvePlaceholders("${java.io.tmpdir}");
		path = path.replace("\\", "/");
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	@Test
	public void baseConfigLocation() {
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.outputCapture.expect(containsString("Hello world"));
		this.outputCapture.expect(not(containsString("???")));
		this.outputCapture.expect(containsString("[junit-"));
		this.logger.info("Hello world", new RuntimeException("Expected"));
		assertThat(new File(tmpDir() + "/spring.log").exists()).isFalse();
	}

	@Test
	public void overrideConfigLocation() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.config=classpath:logback-nondefault.xml");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertThat(output).contains("Hello world").doesNotContain("???")
				.startsWith("LOG_FILE_IS_UNDEFINED").endsWith("BOOTBOOT");
	}

	@Test
	public void overrideConfigDoesNotExist() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.config=doesnotexist.xml");
		this.thrown.expect(IllegalStateException.class);
		this.outputCapture.expect(containsString(
				"Logging system failed to initialize using configuration from 'doesnotexist.xml'"));
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
	}

	@Test
	public void azureDefaultLoggingConfigDoesNotCauseAFailure() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.config: -Djava.util.logging.config.file=\"d:\\home\\site\\wwwroot\\bin\\apache-tomcat-7.0.52\\conf\\logging.properties\"");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertThat(output).contains("Hello world").doesNotContain("???");
		assertThat(new File(tmpDir() + "/spring.log").exists()).isFalse();
	}

	@Test
	public void overrideConfigBroken() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.config=classpath:logback-broken.xml");
		this.thrown.expect(IllegalStateException.class);
		this.outputCapture.expect(containsString(
				"Logging system failed to initialize using configuration from 'classpath:logback-broken.xml'"));
		this.outputCapture.expect(containsString("ConsolAppender"));
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
	}

	@Test
	public void addLogFileProperty() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.config=classpath:logback-nondefault.xml",
				"logging.file=target/foo.log");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertThat(output).startsWith("target/foo.log");
	}

	@Test
	public void addLogFilePropertyWithDefault() {
		assertThat(new File("target/foo.log").exists()).isFalse();
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.file=target/foo.log");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		logger.info("Hello world");
		assertThat(new File("target/foo.log").exists()).isTrue();
	}

	@Test
	public void addLogPathProperty() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.config=classpath:logback-nondefault.xml",
				"logging.path=target/foo/");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertThat(output).startsWith("target/foo/spring.log");
	}

	@Test
	public void parseDebugArg() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, "debug");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString()).contains("testatdebug");
		assertThat(this.outputCapture.toString()).doesNotContain("testattrace");
	}

	@Test
	public void parseTraceArg() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, "trace");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString()).contains("testatdebug");
		assertThat(this.outputCapture.toString()).contains("testattrace");
	}

	@Test
	public void disableDebugArg() {
		disableDebugTraceArg("debug=false");
	}

	@Test
	public void disableTraceArg() {
		disableDebugTraceArg("trace=false");
	}

	private void disableDebugTraceArg(String... environment) {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				environment);
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString()).doesNotContain("testatdebug");
		assertThat(this.outputCapture.toString()).doesNotContain("testattrace");
	}

	@Test
	public void parseLevels() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.level.org.springframework.boot=TRACE");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString()).contains("testatdebug");
		assertThat(this.outputCapture.toString()).contains("testattrace");
	}

	@Test
	public void parseLevelsCaseInsensitive() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.level.org.springframework.boot=TrAcE");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString()).contains("testatdebug");
		assertThat(this.outputCapture.toString()).contains("testattrace");
	}

	@Test
	public void parseLevelsWithPlaceholder() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"foo=TRACE", "logging.level.org.springframework.boot=${foo}");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString()).contains("testatdebug");
		assertThat(this.outputCapture.toString()).contains("testattrace");
	}

	@Test
	public void parseLevelsFails() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.level.org.springframework.boot=GARBAGE");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.outputCapture.toString()).doesNotContain("testatdebug")
				.contains("Cannot set level: GARBAGE");
	}

	@Test
	public void parseLevelsNone() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.level.org.springframework.boot=OFF");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.fatal("testatfatal");
		assertThat(this.outputCapture.toString()).doesNotContain("testatdebug")
				.doesNotContain("testatfatal");
	}

	@Test
	public void parseLevelsMapsFalseToOff() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.level.org.springframework.boot=false");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.fatal("testatfatal");
		assertThat(this.outputCapture.toString()).doesNotContain("testatdebug")
				.doesNotContain("testatfatal");
	}

	@Test
	public void parseArgsDisabled() throws Exception {
		this.initializer.setParseArgs(false);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context, "debug");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.outputCapture.toString()).doesNotContain("testatdebug");
	}

	@Test
	public void parseArgsDoesntReplace() throws Exception {
		this.initializer.setSpringBootLogging(LogLevel.ERROR);
		this.initializer.setParseArgs(false);
		this.initializer.onApplicationEvent(new ApplicationStartedEvent(
				this.springApplication, new String[] { "--debug" }));
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.outputCapture.toString()).doesNotContain("testatdebug");
	}

	@Test
	public void bridgeHandlerLifecycle() throws Exception {
		assertThat(bridgeHandlerInstalled()).isTrue();
		this.initializer.onApplicationEvent(new ContextClosedEvent(this.context));
		assertThat(bridgeHandlerInstalled()).isFalse();
	}

	@Test
	public void defaultExceptionConversionWord() throws Exception {
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.outputCapture.expect(containsString("Hello world"));
		this.outputCapture.expect(
				not(containsString("Wrapped by: java.lang.RuntimeException: Wrapper")));
		this.logger.info("Hello world",
				new RuntimeException("Wrapper", new RuntimeException("Expected")));
	}

	@Test
	public void overrideExceptionConversionWord() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.exceptionConversionWord=%rEx");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.outputCapture.expect(containsString("Hello world"));
		this.outputCapture.expect(
				containsString("Wrapped by: java.lang.RuntimeException: Wrapper"));
		this.logger.info("Hello world",
				new RuntimeException("Wrapper", new RuntimeException("Expected")));
	}

	@Test
	public void shutdownHookIsNotRegisteredByDefault() throws Exception {
		TestLoggingApplicationListener listener = new TestLoggingApplicationListener();
		System.setProperty(LoggingSystem.class.getName(),
				TestShutdownHandlerLoggingSystem.class.getName());
		listener.onApplicationEvent(
				new ApplicationStartedEvent(new SpringApplication(), NO_ARGS));
		listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertThat(listener.shutdownHook).isNull();
	}

	@Test
	public void shutdownHookCanBeRegistered() throws Exception {
		TestLoggingApplicationListener listener = new TestLoggingApplicationListener();
		System.setProperty(LoggingSystem.class.getName(),
				TestShutdownHandlerLoggingSystem.class.getName());
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.register_shutdown_hook=true");
		listener.onApplicationEvent(
				new ApplicationStartedEvent(new SpringApplication(), NO_ARGS));
		listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertThat(listener.shutdownHook).isNotNull();
		listener.shutdownHook.start();
		assertThat(TestShutdownHandlerLoggingSystem.shutdownLatch.await(30,
				TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void closingContextCleansUpLoggingSystem() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY,
				TestCleanupLoggingSystem.class.getName());
		this.initializer.onApplicationEvent(
				new ApplicationStartedEvent(this.springApplication, new String[0]));
		TestCleanupLoggingSystem loggingSystem = (TestCleanupLoggingSystem) ReflectionTestUtils
				.getField(this.initializer, "loggingSystem");
		assertThat(loggingSystem.cleanedUp).isFalse();
		this.initializer.onApplicationEvent(new ContextClosedEvent(this.context));
		assertThat(loggingSystem.cleanedUp).isTrue();
	}

	@Test
	public void closingChildContextDoesNotCleanUpLoggingSystem() {
		System.setProperty(LoggingSystem.SYSTEM_PROPERTY,
				TestCleanupLoggingSystem.class.getName());
		this.initializer.onApplicationEvent(
				new ApplicationStartedEvent(this.springApplication, new String[0]));
		TestCleanupLoggingSystem loggingSystem = (TestCleanupLoggingSystem) ReflectionTestUtils
				.getField(this.initializer, "loggingSystem");
		assertThat(loggingSystem.cleanedUp).isFalse();
		GenericApplicationContext childContext = new GenericApplicationContext();
		childContext.setParent(this.context);
		this.initializer.onApplicationEvent(new ContextClosedEvent(childContext));
		assertThat(loggingSystem.cleanedUp).isFalse();
		this.initializer.onApplicationEvent(new ContextClosedEvent(this.context));
		assertThat(loggingSystem.cleanedUp).isTrue();
		childContext.close();
	}

	@Test
	public void systemPropertiesAreSetForLoggingConfiguration() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.exception-conversion-word=conversion", "logging.file=target/log",
				"logging.path=path", "logging.pattern.console=console",
				"logging.pattern.file=file", "logging.pattern.level=level");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		assertThat(System.getProperty("CONSOLE_LOG_PATTERN")).isEqualTo("console");
		assertThat(System.getProperty("FILE_LOG_PATTERN")).isEqualTo("file");
		assertThat(System.getProperty("LOG_EXCEPTION_CONVERSION_WORD"))
				.isEqualTo("conversion");
		assertThat(System.getProperty("LOG_FILE")).isEqualTo("target/log");
		assertThat(System.getProperty("LOG_LEVEL_PATTERN")).isEqualTo("level");
		assertThat(System.getProperty("LOG_PATH")).isEqualTo("path");
		assertThat(System.getProperty("PID")).isNotNull();
	}

	@Test
	public void logFilePropertiesCanReferenceSystemProperties() {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.context,
				"logging.file=target/${PID}.log");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		assertThat(System.getProperty("LOG_FILE"))
				.isEqualTo("target/" + new ApplicationPid().toString() + ".log");
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

	public static class TestShutdownHandlerLoggingSystem extends AbstractLoggingSystem {

		private static CountDownLatch shutdownLatch;

		public TestShutdownHandlerLoggingSystem(ClassLoader classLoader) {
			super(classLoader);
			TestShutdownHandlerLoggingSystem.shutdownLatch = new CountDownLatch(1);
		}

		@Override
		protected String[] getStandardConfigLocations() {
			return new String[] { "foo.bar" };
		}

		@Override
		protected void loadDefaults(LoggingInitializationContext initializationContext,
				LogFile logFile) {
		}

		@Override
		protected void loadConfiguration(
				LoggingInitializationContext initializationContext, String location,
				LogFile logFile) {
		}

		@Override
		public void setLogLevel(String loggerName, LogLevel level) {

		}

		@Override
		public Runnable getShutdownHandler() {
			return new Runnable() {

				@Override
				public void run() {
					TestShutdownHandlerLoggingSystem.shutdownLatch.countDown();
				}

			};
		}

	}

	public static class TestLoggingApplicationListener
			extends LoggingApplicationListener {

		private Thread shutdownHook;

		@Override
		void registerShutdownHook(Thread shutdownHook) {
			this.shutdownHook = shutdownHook;
		}

	}

	public static final class TestCleanupLoggingSystem extends LoggingSystem {

		private boolean cleanedUp = false;

		public TestCleanupLoggingSystem(ClassLoader classLoader) {

		}

		@Override
		public void beforeInitialize() {

		}

		@Override
		public void setLogLevel(String loggerName, LogLevel level) {

		}

		@Override
		public void cleanUp() {
			this.cleanedUp = true;
		}

	}

}
