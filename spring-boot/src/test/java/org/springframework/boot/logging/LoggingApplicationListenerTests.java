/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.logging.java.JavaLoggingSystem;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.OutputCapture;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.GenericApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link LoggingApplicationListener}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class LoggingApplicationListenerTests {

	private static final String[] NO_ARGS = {};

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

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
		assertFalse(new File(tmpDir() + "/spring.log").exists());
	}

	@Test
	public void overrideConfigLocation() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.config: classpath:logback-nondefault.xml");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertFalse("Wrong output:\n" + output, output.contains("???"));
		assertTrue("Wrong output:\n" + output,
				output.startsWith("LOG_FILE_IS_UNDEFINED"));
		assertTrue("Wrong output:\n" + output, output.endsWith("BOOTBOOT"));
	}

	@Test
	public void overrideConfigDoesNotExist() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.config: doesnotexist.xml");
		this.thrown.expect(IllegalStateException.class);
		this.outputCapture.expect(containsString(
				"Logging system failed to initialize using configuration from 'doesnotexist.xml'"));
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
	}

	@Test
	public void azureDefaultLoggingConfigDoesNotCauseAFailure() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.config: -Djava.util.logging.config.file=\"d:\\home\\site\\wwwroot\\bin\\apache-tomcat-7.0.52\\conf\\logging.properties\"");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertFalse("Wrong output:\n" + output, output.contains("???"));
		assertFalse(new File(tmpDir() + "/spring.log").exists());
	}

	@Test
	public void overrideConfigBroken() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.config: classpath:logback-broken.xml");
		this.thrown.expect(IllegalStateException.class);
		this.outputCapture.expect(containsString(
				"Logging system failed to initialize using configuration from 'classpath:logback-broken.xml'"));
		this.outputCapture.expect(containsString("ConsolAppender"));
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
	}

	@Test
	public void addLogFileProperty() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.config: classpath:logback-nondefault.xml",
				"logging.file: target/foo.log");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertTrue("Wrong output:\n" + output, output.startsWith("target/foo.log"));
	}

	@Test
	public void addLogFilePropertyWithDefault() {
		assertFalse(new File("target/foo.log").exists());
		EnvironmentTestUtils.addEnvironment(this.context, "logging.file: target/foo.log");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		logger.info("Hello world");
		assertTrue(new File("target/foo.log").exists());
	}

	@Test
	public void addLogPathProperty() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.config: classpath:logback-nondefault.xml",
				"logging.path: target/foo/");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertTrue("Wrong output:\n" + output,
				output.startsWith("target/foo/spring.log"));
	}

	@Test
	public void parseDebugArg() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "debug");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString(), containsString("testatdebug"));
		assertThat(this.outputCapture.toString(), not(containsString("testattrace")));
	}

	@Test
	public void parseTraceArg() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "trace");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString(), containsString("testatdebug"));
		assertThat(this.outputCapture.toString(), containsString("testattrace"));
	}

	@Test
	public void parseLevels() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.level.org.springframework.boot=TRACE");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString(), containsString("testatdebug"));
		assertThat(this.outputCapture.toString(), containsString("testattrace"));
	}

	@Test
	public void parseLevelsCaseInsensitive() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.level.org.springframework.boot=TrAcE");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString(), containsString("testatdebug"));
		assertThat(this.outputCapture.toString(), containsString("testattrace"));
	}

	@Test
	public void parseLevelsWithPlaceholder() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "foo=TRACE",
				"logging.level.org.springframework.boot=${foo}");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString(), containsString("testatdebug"));
		assertThat(this.outputCapture.toString(), containsString("testattrace"));
	}

	@Test
	public void parseLevelsFails() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.level.org.springframework.boot=GARBAGE");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.outputCapture.toString(), not(containsString("testatdebug")));
		assertThat(this.outputCapture.toString(),
				containsString("Cannot set level: GARBAGE"));
	}

	@Test
	public void parseLevelsNone() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.level.org.springframework.boot=OFF");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.fatal("testatfatal");
		assertThat(this.outputCapture.toString(), not(containsString("testatdebug")));
		assertThat(this.outputCapture.toString(), not(containsString("testatfatal")));
	}

	@Test
	public void parseLevelsMapsFalseToOff() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.level.org.springframework.boot=false");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.fatal("testatfatal");
		assertThat(this.outputCapture.toString(), not(containsString("testatdebug")));
		assertThat(this.outputCapture.toString(), not(containsString("testatfatal")));
	}

	@Test
	public void parseArgsDisabled() throws Exception {
		this.initializer.setParseArgs(false);
		EnvironmentTestUtils.addEnvironment(this.context, "debug");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.outputCapture.toString(), not(containsString("testatdebug")));
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
		assertThat(this.outputCapture.toString(), not(containsString("testatdebug")));
	}

	@Test
	public void bridgeHandlerLifecycle() throws Exception {
		assertTrue(bridgeHandlerInstalled());
		this.initializer.onApplicationEvent(new ContextClosedEvent(this.context));
		assertFalse(bridgeHandlerInstalled());
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
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.exceptionConversionWord:%rEx");
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
		assertThat(listener.shutdownHook, is(nullValue()));
	}

	@Test
	public void shutdownHookCanBeRegistered() throws Exception {
		TestLoggingApplicationListener listener = new TestLoggingApplicationListener();
		System.setProperty(LoggingSystem.class.getName(),
				TestShutdownHandlerLoggingSystem.class.getName());
		EnvironmentTestUtils.addEnvironment(this.context,
				"logging.register_shutdown_hook:true");
		listener.onApplicationEvent(
				new ApplicationStartedEvent(new SpringApplication(), NO_ARGS));
		listener.initialize(this.context.getEnvironment(), this.context.getClassLoader());
		assertThat(listener.shutdownHook, is(not(nullValue())));
		listener.shutdownHook.start();
		assertThat(TestShutdownHandlerLoggingSystem.shutdownLatch.await(30,
				TimeUnit.SECONDS), is(true));
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

}
