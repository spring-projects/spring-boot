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

package org.springframework.boot.logging.log4j2;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.FileConfigurationMonitor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.logging.AbstractLoggingSystemTests;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.OutputCapture;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link Log4J2LoggingSystem}.
 *
 * @author Daniel Fullarton
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class Log4J2LoggingSystemTests extends AbstractLoggingSystemTests {

	@Rule
	public OutputCapture output = new OutputCapture();

	private final TestLog4J2LoggingSystem loggingSystem = new TestLog4J2LoggingSystem();

	private Logger logger;

	@Before
	public void setup() {
		this.logger = LogManager.getLogger(getClass());
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
		assertThat(this.loggingSystem.getConfiguration().getConfigurationSource()
				.getFile(), is(notNullValue()));
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
		assertThat(this.loggingSystem.getConfiguration().getConfigurationSource()
				.getFile(), is(notNullValue()));
	}

	@Test
	public void testNonDefaultConfigLocation() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize("classpath:log4j2-nondefault.xml",
				getLogFile(tmpDir() + "/tmp.log", null));
		this.logger.info("Hello world");
		String output = this.output.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
		assertTrue("Wrong output:\n" + output, output.contains(tmpDir() + "/tmp.log"));
		assertFalse(new File(tmpDir() + "/tmp.log").exists());
		assertThat(this.loggingSystem.getConfiguration().getConfigurationSource()
				.getFile().getAbsolutePath(), containsString("log4j2-nondefault.xml"));
		// we assume that "log4j2-nondefault.xml" contains the 'monitorInterval'
		// attribute, so we check that a monitor is created
		assertThat(this.loggingSystem.getConfiguration().getConfigurationMonitor(),
				is(instanceOf(FileConfigurationMonitor.class)));
	}

	@Test(expected = IllegalStateException.class)
	public void testNonexistentConfigLocation() throws Exception {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize("classpath:log4j2-nonexistent.xml", null);
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
	@Ignore("Fails on Bamboo")
	public void loggingThatUsesJulIsCaptured() {
		this.loggingSystem.beforeInitialize();
		this.loggingSystem.initialize(null, null);
		java.util.logging.Logger julLogger = java.util.logging.Logger
				.getLogger(getClass().getName());
		julLogger.severe("Hello world");
		String output = this.output.toString().trim();
		assertTrue("Wrong output:\n" + output, output.contains("Hello world"));
	}

	@Test
	public void configLocationsWithNoExtraDependencies() {
		assertThat(this.loggingSystem.getStandardConfigLocations(),
				is(arrayContaining("log4j2.xml")));
	}

	@Test
	public void configLocationsWithJacksonDatabind() {
		this.loggingSystem.availableClasses(ObjectMapper.class.getName());
		assertThat(this.loggingSystem.getStandardConfigLocations(),
				is(arrayContaining("log4j2.json", "log4j2.jsn", "log4j2.xml")));
	}

	@Test
	public void configLocationsWithJacksonDataformatYaml() {
		this.loggingSystem
				.availableClasses("com.fasterxml.jackson.dataformat.yaml.YAMLParser");
		assertThat(this.loggingSystem.getStandardConfigLocations(),
				is(arrayContaining("log4j2.yaml", "log4j2.yml", "log4j2.xml")));
	}

	@Test
	public void configLocationsWithJacksonDatabindAndDataformatYaml() {
		this.loggingSystem.availableClasses(
				"com.fasterxml.jackson.dataformat.yaml.YAMLParser",
				ObjectMapper.class.getName());
		assertThat(
				this.loggingSystem.getStandardConfigLocations(),
				is(arrayContaining("log4j2.yaml", "log4j2.yml", "log4j2.json",
						"log4j2.jsn", "log4j2.xml")));
	}

	private static class TestLog4J2LoggingSystem extends Log4J2LoggingSystem {

		private List<String> availableClasses = new ArrayList<String>();

		public TestLog4J2LoggingSystem() {
			super(TestLog4J2LoggingSystem.class.getClassLoader());
		}

		public Configuration getConfiguration() {
			return ((org.apache.logging.log4j.core.LoggerContext) LogManager
					.getContext(false)).getConfiguration();
		}

		@Override
		protected boolean isClassAvailable(String className) {
			return this.availableClasses.contains(className);
		}

		private void availableClasses(String... classNames) {
			Collections.addAll(this.availableClasses, classNames);
		}

	}

}
