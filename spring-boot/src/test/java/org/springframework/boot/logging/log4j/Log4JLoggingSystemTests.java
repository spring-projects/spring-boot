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

package org.springframework.boot.logging.log4j;

import org.apache.commons.logging.impl.Log4JLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.OutputCapture;
import org.springframework.util.StringUtils;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link Log4JLoggingSystem}.
 *
 * @author Phillip Webb
 */
public class Log4JLoggingSystemTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private final Log4JLoggingSystem loggingSystem = new Log4JLoggingSystem(getClass()
			.getClassLoader());

	private Log4JLogger logger;

	@Before
	public void setup() {
		this.logger = new Log4JLogger(getClass().getName());
	}

	@After
	public void clear() {
		System.clearProperty("LOG_FILE");
		System.clearProperty("LOG_PATH");
		System.clearProperty("PID");
	}

	@Test
	public void testNonDefaultConfigLocation() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize("classpath:log4j-nondefault.properties");
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertTrue("Wrong output:\n" + output, output.contains("/tmp/spring.log"));
	}

	@Test(expected = IllegalStateException.class)
	public void testNonexistentConfigLocation() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize("classpath:log4j-nonexistent.xml");
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

	@Test
	@Ignore("Fails on Bamboo")
	public void loggingThatUsesJulIsCaptured() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize();
		java.util.logging.Logger julLogger = java.util.logging.Logger
				.getLogger(getClass().getName());
		julLogger.severe("Hello world");
		String output = this.output.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
	}

}
