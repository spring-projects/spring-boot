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

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.impl.StaticLoggerBinder;

import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.OutputCapture;
import org.springframework.mock.env.MockEnvironment;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link SpringBootJoranConfigurator}.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 */
public class SpringBootJoranConfiguratorTests {

	@Rule
	public OutputCapture out = new OutputCapture();

	private MockEnvironment environment;

	private LoggingInitializationContext initializationContext;

	private JoranConfigurator configurator;

	private LoggerContext context;

	private Logger logger;

	@Before
	public void setup() {
		this.environment = new MockEnvironment();
		this.initializationContext = new LoggingInitializationContext(this.environment);
		this.configurator = new SpringBootJoranConfigurator(this.initializationContext);
		StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
		this.context = (LoggerContext) binder.getLoggerFactory();
		this.logger = this.context.getLogger(getClass());
	}

	@After
	public void reset() {
		this.context.reset();
	}

	@Test
	public void profileActive() throws Exception {
		this.environment.setActiveProfiles("production");
		initialize("production-profile.xml");
		this.logger.trace("Hello");
		this.out.expect(containsString("Hello"));
	}

	@Test
	public void multipleNamesFirstProfileActive() throws Exception {
		this.environment.setActiveProfiles("production");
		initialize("multi-profile-names.xml");
		this.logger.trace("Hello");
		this.out.expect(containsString("Hello"));
	}

	@Test
	public void multipleNamesSecondProfileActive() throws Exception {
		this.environment.setActiveProfiles("test");
		initialize("multi-profile-names.xml");
		this.logger.trace("Hello");
		this.out.expect(containsString("Hello"));
	}

	@Test
	public void profileNotActive() throws Exception {
		initialize("production-profile.xml");
		this.logger.trace("Hello");
		this.out.expect(not(containsString("Hello")));
	}

	@Test
	public void profileNestedActiveActive() throws Exception {
		doTestNestedProfile(true, "outer", "inner");
	}

	@Test
	public void profileNestedActiveNotActive() throws Exception {
		doTestNestedProfile(false, "outer");
	}

	@Test
	public void profileNestedNotActiveActive() throws Exception {
		doTestNestedProfile(false, "inner");
	}

	@Test
	public void profileNestedNotActiveNotActive() throws Exception {
		doTestNestedProfile(false);
	}

	@Test
	public void springProperty() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.environment, "my.example-property:test");
		initialize("property.xml");
		assertThat(this.context.getProperty("MINE"), equalTo("test"));
	}

	@Test
	public void relaxedSpringProperty() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.environment, "my.EXAMPLE_PROPERTY:test");
		initialize("property.xml");
		assertThat(this.context.getProperty("MINE"), equalTo("test"));
	}

	private void doTestNestedProfile(boolean expected, String... profiles)
			throws JoranException {
		this.environment.setActiveProfiles(profiles);
		initialize("nested.xml");
		this.logger.trace("Hello");
		if (expected) {
			this.out.expect(containsString("Hello"));
		}
		else {
			this.out.expect(not(containsString("Hello")));
		}

	}

	private void initialize(String config) throws JoranException {
		this.configurator.setContext(this.context);
		this.configurator.doConfigure(getClass().getResourceAsStream(config));
	}

}
