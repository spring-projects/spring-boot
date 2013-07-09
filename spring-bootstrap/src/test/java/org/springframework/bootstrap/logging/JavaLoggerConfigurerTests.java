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

package org.springframework.bootstrap.logging;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.LogManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link JavaLoggerConfigurer}.
 * 
 * @author Dave Syer
 */
public class JavaLoggerConfigurerTests {

	private PrintStream savedOutput;
	private ByteArrayOutputStream output;

	@Before
	public void init() throws SecurityException, IOException {
		this.savedOutput = System.out;
		this.output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(this.output));
		LogManager.getLogManager().readConfiguration(
				getClass().getResourceAsStream("logging.properties"));
	}

	@After
	public void clear() {
		System.clearProperty("LOG_FILE");
		System.clearProperty("LOG_PATH");
		System.clearProperty("PID");
		System.setOut(this.savedOutput);
	}

	private String getOutput() {
		return this.output.toString();
	}

	@Test
	public void testDefaultConfigLocation() throws Exception {
		JavaLoggerConfigurer.initLogging("classpath:logging-nondefault.properties");
		Log logger = LogFactory.getLog(JavaLoggerConfigurerTests.class);
		logger.info("Hello world");
		String output = getOutput().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertTrue("Wrong output:\n" + output, output.startsWith("["));
	}

	@Test(expected = FileNotFoundException.class)
	public void testNonexistentConfigLocation() throws Exception {
		JavaLoggerConfigurer.initLogging("classpath:logging-nonexistent.properties");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullConfigLocation() throws Exception {
		JavaLoggerConfigurer.initLogging(null);
	}

}
