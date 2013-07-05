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

package org.springframework.zero.logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.zero.context.initializer.LoggingApplicationContextInitializer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class LoggingInitializerTests {

	private LoggingApplicationContextInitializer initializer = new LoggingApplicationContextInitializer();

	private PrintStream savedOutput;

	private ByteArrayOutputStream output;

	@Before
	public void init() {
		this.savedOutput = System.err;
		this.output = new ByteArrayOutputStream();
		System.setOut(new PrintStream(this.output));
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
	public void testDefaultConfigLocation() {
		GenericApplicationContext context = new GenericApplicationContext();
		this.initializer.initialize(context);
		Log logger = LogFactory.getLog(LoggingInitializerTests.class);
		logger.info("Hello world");
		String output = getOutput().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertFalse("Wrong output:\n" + output, output.contains("???"));
		assertTrue("Wrong output:\n" + output, output.startsWith("["));
	}

	@Test
	public void testOverrideConfigLocation() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.getEnvironment().getPropertySources()
				.addFirst(new PropertySource<String>("manual") {
					@Override
					public Object getProperty(String name) {
						if ("logging.config".equals(name)) {
							return "classpath:logback-nondefault.xml";
						}
						return null;
					}
				});
		this.initializer.initialize(context);
		Log logger = LogFactory.getLog(LoggingInitializerTests.class);
		logger.info("Hello world");
		String output = getOutput().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertFalse("Wrong output:\n" + output, output.contains("???"));
		assertTrue("Wrong output:\n" + output, output.startsWith("/tmp/springzero.log"));
	}

	@Test
	public void testAddLogFileProperty() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.getEnvironment().getPropertySources()
				.addFirst(new PropertySource<String>("manual") {
					@Override
					public Object getProperty(String name) {
						if ("logging.config".equals(name)) {
							return "classpath:logback-nondefault.xml";
						}
						if ("logging.file".equals(name)) {
							return "foo.log";
						}
						return null;
					}
				});
		this.initializer.initialize(context);
		Log logger = LogFactory.getLog(LoggingInitializerTests.class);
		logger.info("Hello world");
		String output = getOutput().trim();
		assertTrue("Wrong output:\n" + output, output.startsWith("foo.log"));
	}

	@Test
	public void testAddLogPathProperty() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.getEnvironment().getPropertySources()
				.addFirst(new PropertySource<String>("manual") {
					@Override
					public Object getProperty(String name) {
						if ("logging.config".equals(name)) {
							return "classpath:logback-nondefault.xml";
						}
						if ("logging.path".equals(name)) {
							return "foo/";
						}
						return null;
					}
				});
		this.initializer.initialize(context);
		Log logger = LogFactory.getLog(LoggingInitializerTests.class);
		logger.info("Hello world");
		String output = getOutput().trim();
		assertTrue("Wrong output:\n" + output, output.startsWith("foo/springzero.log"));
	}

}
