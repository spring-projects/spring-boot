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

package org.springframework.boot.logging.logback;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.SLF4JLogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.boot.logging.AbstractLoggingSystemTests;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.OutputCapture;
import org.springframework.util.StringUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link LogbackLoggingSystem}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class LogbackLoggingSystemTests extends AbstractLoggingSystemTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private final LogbackLoggingSystem loggingSystem = new LogbackLoggingSystem(
			getClass().getClassLoader());

	private Log logger;

	@Before
	public void setup() {
		this.logger = new SLF4JLogFactory().getInstance(getClass().getName());
	}

	@Override
	@After
	public void clear() {
		this.loggingSystem.cleanUp();
	}

	@Test
	public void noFile() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(null, null);
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertFalse("Output not hidden:\n" + output, output.contains("Hidden"));
		assertFalse(new File(tmpDir() + "/spring.log").exists());
	}

	@Test
	public void withFile() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.logger.info("Hidden");
		this.loggingSystem.initialize(null, getLogFile(null, tmpDir()));
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertFalse("Output not hidden:\n" + output, output.contains("Hidden"));
		assertTrue(new File(tmpDir() + "/spring.log").exists());
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
		this.loggingSystem.initialize("classpath:logback-nondefault.xml",
				getLogFile(tmpDir() + "/tmp.log", null));
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertTrue("Wrong output:\n" + output, output.contains(tmpDir() + "/tmp.log"));
		assertTrue("Wrong output:\n" + output, output.endsWith("BOOTBOOT"));
		assertFalse(new File(tmpDir() + "/tmp.log").exists());
	}

	@Test(expected = IllegalStateException.class)
	public void testNonexistentConfigLocation() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize("classpath:logback-nonexistent.xml", null);
	}

	@Test
	public void setLevel() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null);
		this.logger.debug("Hello");
		this.loggingSystem.setLogLevel("org.springframework.boot", LogLevel.DEBUG);
		this.logger.debug("Hello");
		assertThat(StringUtils.countOccurrencesOf(this.output.toString(), "Hello"),
				equalTo(1));
	}

	@Test
	public void loggingThatUsesJulIsCaptured() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null);
		java.util.logging.Logger julLogger = java.util.logging.Logger
				.getLogger(getClass().getName());
		julLogger.info("Hello world");
		String output = this.output.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
	}

	@Test
	public void jbossLoggingIsConfiguredToUseSlf4j() {
		this.loggingSystem.beforeInitialize();
		assertEquals("slf4j", System.getProperty("org.jboss.logging.provider"));
	}

	@Test
	public void bridgeHandlerLifecycle() {
		assertFalse(bridgeHandlerInstalled());
		this.loggingSystem.beforeInitialize();
		assertTrue(bridgeHandlerInstalled());
		this.loggingSystem.cleanUp();
		assertFalse(bridgeHandlerInstalled());
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

}
