/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SLF4JLogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.OutputCapture;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link LogbackLoggingSystem}.
 * 
 * @author Dave Syer
 */
public class LogbackLoggingSystemTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private final LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(
			getClass().getClassLoader());

	private Log logger;

	@Before
	public void setup() {
		this.logger = new SLF4JLogFactory().getInstance(getClass().getName());
		new File(tmpDir() + "/spring.log").delete();
	}

	private String tmpDir() {
		return System.getProperty("java.io.tmpdir");
	}

	@After
	public void clear() {
		System.clearProperty("LOG_FILE");
		System.clearProperty("LOG_PATH");
		System.clearProperty("PID");
	}

	@Test
	public void testBasicConfigLocation() throws Exception {
		this.loggingSystem.beforeInitialize();
		ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
		LoggerContext context = (LoggerContext) factory;
		Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		assertNotNull(root.getAppender("CONSOLE"));
	}

	@Test
	public void testNonDefaultConfigLocation() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize("classpath:logback-nondefault.xml");
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertTrue("Wrong output (not " + tmpDir() + " :\n" + output,
				output.contains(tmpDir() + "/tmp.log"));
		assertFalse(new File(tmpDir() + "/tmp.log").exists());
	}

	@Test(expected = IllegalStateException.class)
	public void testNonexistentConfigLocation() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize("classpath:logback-nonexistent.xml");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullConfigLocation() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null);
	}

	@Test
	public void setLevel() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize();
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.debug("Hello");
		assertThat(StringUtils.countOccurrencesOf(this.output.toString(), "Hello"),
				equalTo(1));
	}

}
