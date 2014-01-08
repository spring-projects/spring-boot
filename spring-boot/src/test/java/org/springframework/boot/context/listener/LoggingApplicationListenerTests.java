/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.context.listener;

import java.io.IOException;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SLF4JLogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.OutputCapture;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationStartEvent;
import org.springframework.boot.SpringBootTestUtils;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.java.JavaLoggingSystem;
import org.springframework.context.support.GenericApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link LoggingApplicationListener}.
 * 
 * @author Dave Syer
 * @author Phillip Webb
 */
public class LoggingApplicationListenerTests {

	private static final String[] NO_ARGS = {};

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	private LoggingApplicationListener initializer = new LoggingApplicationListener();

	private Log logger = new SLF4JLogFactory().getInstance(getClass());

	private SpringApplication springApplication = new SpringApplication();

	private GenericApplicationContext context = new GenericApplicationContext();

	@Before
	public void init() throws SecurityException, IOException {
		LogManager.getLogManager().readConfiguration(
				JavaLoggingSystem.class.getResourceAsStream("logging.properties"));
		this.initializer.onApplicationEvent(new SpringApplicationStartEvent(
				new SpringApplication(), NO_ARGS));
	}

	@After
	public void clear() {
		System.clearProperty("LOG_FILE");
		System.clearProperty("LOG_PATH");
		System.clearProperty("PID");
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultConfigLocation() {
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertFalse("Wrong output:\n" + output, output.contains("???"));
	}

	@Test
	public void testOverrideConfigLocation() {
		SpringBootTestUtils.addEnviroment(this.context,
				"logging.config: classpath:logback-nondefault.xml");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertFalse("Wrong output:\n" + output, output.contains("???"));
		assertTrue("Wrong output:\n" + output, output.startsWith("/tmp/spring.log"));
	}

	@Test
	public void testOverrideConfigDoesNotExist() throws Exception {
		SpringBootTestUtils.addEnviroment(this.context, "logging.config: doesnotexist.xml");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		// Should not throw
	}

	@Test
	public void testAddLogFileProperty() {
		SpringBootTestUtils.addEnviroment(this.context,
				"logging.config: classpath:logback-nondefault.xml",
				"logging.file: foo.log");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertTrue("Wrong output:\n" + output, output.startsWith("foo.log"));
	}

	@Test
	public void testAddLogPathProperty() {
		SpringBootTestUtils.addEnviroment(this.context,
				"logging.config: classpath:logback-nondefault.xml", "logging.path: foo/");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		Log logger = LogFactory.getLog(LoggingApplicationListenerTests.class);
		logger.info("Hello world");
		String output = this.outputCapture.toString().trim();
		assertTrue("Wrong output:\n" + output, output.startsWith("foo/spring.log"));
	}

	@Test
	public void parseDebugArg() throws Exception {
		SpringBootTestUtils.addEnviroment(this.context, "debug");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString(), containsString("testatdebug"));
		assertThat(this.outputCapture.toString(), not(containsString("testattrace")));
	}

	@Test
	public void parseTraceArg() throws Exception {
		SpringBootTestUtils.addEnviroment(this.context, "trace");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		this.logger.trace("testattrace");
		assertThat(this.outputCapture.toString(), containsString("testatdebug"));
		assertThat(this.outputCapture.toString(), containsString("testattrace"));
	}

	@Test
	public void parseArgsDisabled() throws Exception {
		this.initializer.setParseArgs(false);
		SpringBootTestUtils.addEnviroment(this.context, "debug");
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.outputCapture.toString(), not(containsString("testatdebug")));
	}

	@Test
	public void parseArgsDoesntReplace() throws Exception {
		this.initializer.setSpringBootLogging(LogLevel.ERROR);
		this.initializer.setParseArgs(false);
		this.initializer.onApplicationEvent(new SpringApplicationStartEvent(
				this.springApplication, new String[] { "--debug" }));
		this.initializer.initialize(this.context.getEnvironment(),
				this.context.getClassLoader());
		this.logger.debug("testatdebug");
		assertThat(this.outputCapture.toString(), not(containsString("testatdebug")));
	}
}
