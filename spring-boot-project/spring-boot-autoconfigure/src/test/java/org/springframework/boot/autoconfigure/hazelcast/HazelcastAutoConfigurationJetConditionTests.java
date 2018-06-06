/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

public class HazelcastAutoConfigurationJetConditionTests {

	private static final String HAZELCAST_CLIENT_XML_FILE = "hazelcast-client.xml";

	private static final String HAZELCAST_JET_XML_FILE = "hazelcast-jet.xml";

	private static final String HAZELCAST_XML_FILE = "hazelcast.xml";

	private static final String JET_CLIENT_CONFIGURATION_BEAN_NAME = "JetClientConfiguration";

	private static final String JET_SERVER_CONFIGURATION_BEAN_NAME = "JetServerConfiguration";

	private static final String[] HAZELCAST_FILES = { HAZELCAST_CLIENT_XML_FILE,
			HAZELCAST_JET_XML_FILE, HAZELCAST_XML_FILE };

	private static final Map<String, String> temporaryFilenames = new HashMap<>();

	private static final ClassLoader classLoader = HazelcastAutoConfigurationJetConditionTests.class
			.getClassLoader();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner();

	private ClassLoader testClassLoader;

	/**
	 * <p>
	 * The various {@code @Test} methods in this class try combinations of presence and
	 * absence of Hazelcast files.
	 * </p>
	 * <p>
	 * To avoid side effects, look for these files in "{@code target/test-classes}" first
	 * and temporarily rename them.
	 * </p>
	 * <p>
	 * For instance, "{@code src/test/resources/hazelcast.xml}".
	 * </p>
	 * @throws Exception Security may block renaming
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		for (String originalFilename : HAZELCAST_FILES) {
			URL url = classLoader.getResource(originalFilename);

			if (url != null) {
				File file = new File(url.getFile());

				// Use the class that effected the change as the prefix
				String temporaryFilename = file.getParentFile().getAbsolutePath()
						+ File.separator
						+ HazelcastAutoConfigurationJetConditionTests.class
								.getSimpleName()
						+ "." + originalFilename;
				File destination = new File(temporaryFilename);

				boolean success = file.renameTo(destination);

				if (!success) {
					throw new Exception("Cannot rename " + file.getAbsolutePath() + " to "
							+ temporaryFilename);
				}
				else {
					temporaryFilenames.put(destination.getAbsolutePath(),
							file.getAbsolutePath());
				}
			}
		}
	}

	/**
	 * <p>
	 * Reverse the file renaming done by {@code @BeforeClass}
	 * </p>
	 * @throws Exception Shouldn't occur if {@code @BeforeClass} was successful
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		for (Entry<String, String> entry : temporaryFilenames.entrySet()) {
			String temporaryFilename = entry.getKey();
			String originalFilename = entry.getValue();

			File file = new File(temporaryFilename);

			if (!file.exists()) {
				throw new Exception("Cannot find " + temporaryFilename);
			}

			File destination = new File(originalFilename);

			boolean success = file.renameTo(destination);

			if (!success) {
				throw new Exception(
						"Cannot rename " + temporaryFilename + " to " + originalFilename);
			}
		}
	}

	/**
	 * <p>
	 * Create a classloader that looks in JUnit's temporary folder.
	 * </p>
	 * @throws Exception Unexpected if it does occur
	 */
	@Before
	public void setUp() throws Exception {
		ClassLoader applicationClassLoader = this.applicationContextRunner.getClass()
				.getClassLoader();

		URL[] urls = new URL[1];
		urls[0] = this.temporaryFolder.getRoot().toURI().toURL();

		this.testClassLoader = new URLClassLoader(urls, applicationClassLoader);
	}

	@Test
	public void no_xml_file() {
		this.getApplicationContextRunner().run(this.beanFound("not found", false,
				HAZELCAST_XML_FILE, HAZELCAST_CLIENT_XML_FILE, HAZELCAST_JET_XML_FILE,
				JET_CLIENT_CONFIGURATION_BEAN_NAME, JET_SERVER_CONFIGURATION_BEAN_NAME));
	}

	@Test
	public void hazelcast_xml_file() throws Exception {
		this.createFilesOnClasspath(HAZELCAST_XML_FILE);

		this.getApplicationContextRunner()
				.run(this.beanFound("found", true, HAZELCAST_XML_FILE,
						JET_SERVER_CONFIGURATION_BEAN_NAME))
				.run(this.beanFound("not found", false, HAZELCAST_CLIENT_XML_FILE,
						HAZELCAST_JET_XML_FILE, JET_CLIENT_CONFIGURATION_BEAN_NAME));
	}

	@Test
	public void hazelcast_client_xml_file() throws Exception {
		this.createFilesOnClasspath(HAZELCAST_CLIENT_XML_FILE);

		this.getApplicationContextRunner()
				.run(this.beanFound("found", true, HAZELCAST_CLIENT_XML_FILE,
						JET_CLIENT_CONFIGURATION_BEAN_NAME))
				.run(this.beanFound("not found", false, HAZELCAST_XML_FILE,
						HAZELCAST_JET_XML_FILE, JET_SERVER_CONFIGURATION_BEAN_NAME));
	}

	@Test
	public void hazelcast_jet_xml_file() throws Exception {
		this.createFilesOnClasspath(HAZELCAST_JET_XML_FILE);

		this.getApplicationContextRunner()
				.run(this.beanFound("found", true, HAZELCAST_JET_XML_FILE,
						JET_SERVER_CONFIGURATION_BEAN_NAME))
				.run(this.beanFound("not found", false, HAZELCAST_XML_FILE,
						HAZELCAST_CLIENT_XML_FILE, JET_CLIENT_CONFIGURATION_BEAN_NAME));
	}

	@Test
	public void hazelcast_xml_and_hazelcast_client_xml_file() throws Exception {
		this.createFilesOnClasspath(HAZELCAST_XML_FILE, HAZELCAST_CLIENT_XML_FILE);

		this.getApplicationContextRunner()
				.run(this.beanFound("found", true, HAZELCAST_XML_FILE,
						HAZELCAST_CLIENT_XML_FILE))
				.run(this.beanFound("not found", false, HAZELCAST_JET_XML_FILE,
						JET_CLIENT_CONFIGURATION_BEAN_NAME,
						JET_SERVER_CONFIGURATION_BEAN_NAME));
	}

	@Test
	public void hazelcast_xml_and_hazelcast_jet_xml_file() throws Exception {
		this.createFilesOnClasspath(HAZELCAST_XML_FILE, HAZELCAST_JET_XML_FILE);

		this.getApplicationContextRunner()
				.run(this.beanFound("found", true, HAZELCAST_XML_FILE,
						HAZELCAST_JET_XML_FILE, JET_SERVER_CONFIGURATION_BEAN_NAME))
				.run(this.beanFound("not found", false, HAZELCAST_CLIENT_XML_FILE,
						JET_CLIENT_CONFIGURATION_BEAN_NAME));
	}

	@Test
	public void hazelcast_client_xml_and_hazelcast_jet_xml_file() throws Exception {
		this.createFilesOnClasspath(HAZELCAST_CLIENT_XML_FILE, HAZELCAST_JET_XML_FILE);

		this.getApplicationContextRunner()
				.run(this.beanFound("found", true, HAZELCAST_CLIENT_XML_FILE,
						HAZELCAST_JET_XML_FILE))
				.run(this.beanFound("not found", false, HAZELCAST_XML_FILE,
						JET_CLIENT_CONFIGURATION_BEAN_NAME,
						JET_SERVER_CONFIGURATION_BEAN_NAME));
	}

	@Test
	public void hazelcast_xml_and_hazelcast_client_xml_and_hazelcast_jet_xml_file()
			throws Exception {
		this.createFilesOnClasspath(HAZELCAST_XML_FILE, HAZELCAST_CLIENT_XML_FILE,
				HAZELCAST_JET_XML_FILE);

		this.getApplicationContextRunner()
				.run(this.beanFound("found", true, HAZELCAST_XML_FILE,
						HAZELCAST_CLIENT_XML_FILE, HAZELCAST_JET_XML_FILE))
				.run(this.beanFound("not found", false,
						JET_CLIENT_CONFIGURATION_BEAN_NAME,
						JET_SERVER_CONFIGURATION_BEAN_NAME));
	}

	/**
	 * <p>
	 * Create empty files on the classpath in the temporary folder.
	 * </p>
	 * @param names List of file names to make
	 */
	private void createFilesOnClasspath(String... names) throws IOException {
		for (String name : names) {
			this.temporaryFolder.newFile(name);
		}
	}

	/**
	 * <p>
	 * Test if Spring's application context has a bean with a given name.
	 * </p>
	 * @param prefix Start any assertion fail
	 * @param expected Whether to reject if not found or found
	 * @param beanNames Bean name varargs to look for
	 */
	private ContextConsumer<AssertableApplicationContext> beanFound(String description,
			boolean expected, String... beanNames) {
		return (assertableApplicationContext) -> {
			if (expected) {
				for (String beanName : beanNames) {
					Assertions.assertThat(assertableApplicationContext).as(description)
							.hasBean(beanName);
				}
			}
			else {
				for (String beanName : beanNames) {
					Assertions.assertThat(assertableApplicationContext).as(description)
							.doesNotHaveBean(beanName);
				}
			}
		};
	}

	/**
	 * <p>
	 * Build an application context runner to use in tests that attempts to pick up the
	 * configuration classes. If their conditions aren't met they won't be added.
	 * </p>
	 * @return Application context runner for one Junit test
	 */
	private ApplicationContextRunner getApplicationContextRunner() {
		return this.applicationContextRunner.withClassLoader(this.testClassLoader)
				// Config to test
				.withUserConfiguration(
						HazelcastJetConfiguration.ClientXMLAvailableCondition.class)
				.withUserConfiguration(
						HazelcastJetConfiguration.IMDGServerXMLAvailableCondition.class)
				.withUserConfiguration(
						HazelcastJetConfiguration.JetServerXMLAvailableCondition.class)
				.withUserConfiguration(
						HazelcastJetConfiguration.NoClientXMLAvailableCondition.class)
				.withUserConfiguration(
						HazelcastJetConfiguration.NoServerXMLAvailableCondition.class)
				.withUserConfiguration(
						HazelcastJetConfiguration.ServerXMLAvailableCondition.class)
				.withUserConfiguration(
						HazelcastJetConfiguration.JetClientConfigurationAvailableCondition.class)
				.withUserConfiguration(
						HazelcastJetConfiguration.JetServerConfigurationAvailableCondition.class)
				// Test helper config
				.withUserConfiguration(
						HazelcastAutoConfigurationJetConditionTests.HazelcastXml.class)
				.withUserConfiguration(
						HazelcastAutoConfigurationJetConditionTests.HazelcastClientXml.class)
				.withUserConfiguration(
						HazelcastAutoConfigurationJetConditionTests.HazelcastJetXml.class)
				.withUserConfiguration(
						HazelcastAutoConfigurationJetConditionTests.JetClientConfiguration.class)
				.withUserConfiguration(
						HazelcastAutoConfigurationJetConditionTests.JetServerConfiguration.class);
	}

	// "hazelcast-client.xml" exists
	@Configuration
	@Conditional(HazelcastJetConfiguration.ClientXMLAvailableCondition.class)
	static class HazelcastClientXml {

		@Bean(name = HAZELCAST_CLIENT_XML_FILE)
		public String junit() {
			return HAZELCAST_CLIENT_XML_FILE;
		}

	}

	// "hazelcast.xml" exists
	@Configuration
	@Conditional(HazelcastJetConfiguration.IMDGServerXMLAvailableCondition.class)
	static class HazelcastXml {

		@Bean(name = HAZELCAST_XML_FILE)
		public String junit() {
			return HAZELCAST_XML_FILE;
		}

	}

	// "hazelcast-jet.xml" exists
	@Configuration
	@Conditional(HazelcastJetConfiguration.JetServerXMLAvailableCondition.class)
	static class HazelcastJetXml {

		@Bean(name = HAZELCAST_JET_XML_FILE)
		public String junit() {
			return HAZELCAST_JET_XML_FILE;
		}

	}

	// Nested conditions from HazelcastJetConfiguration
	@Configuration
	@Conditional(HazelcastJetConfiguration.JetClientConfigurationAvailableCondition.class)
	static class JetClientConfiguration {

		@Bean(name = JET_CLIENT_CONFIGURATION_BEAN_NAME)
		public String junit() {
			return JET_CLIENT_CONFIGURATION_BEAN_NAME;
		}

	}

	@Configuration
	@Conditional(HazelcastJetConfiguration.JetServerConfigurationAvailableCondition.class)
	static class JetServerConfiguration {

		@Bean(name = JET_SERVER_CONFIGURATION_BEAN_NAME)
		public String junit() {
			return JET_SERVER_CONFIGURATION_BEAN_NAME;
		}

	}

}
