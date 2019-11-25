/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootJoranConfigurator}.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@ExtendWith(OutputCaptureExtension.class)
class SpringBootJoranConfiguratorTests {

	private MockEnvironment environment;

	private LoggingInitializationContext initializationContext;

	private JoranConfigurator configurator;

	private LoggerContext context;

	private Logger logger;

	private CapturedOutput output;

	@BeforeEach
	void setup(CapturedOutput output) {
		this.output = output;
		this.environment = new MockEnvironment();
		this.initializationContext = new LoggingInitializationContext(this.environment);
		this.configurator = new SpringBootJoranConfigurator(this.initializationContext);
		StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
		this.context = (LoggerContext) binder.getLoggerFactory();
		this.logger = this.context.getLogger(getClass());
	}

	@AfterEach
	void reset() {
		this.context.stop();
		new BasicConfigurator().configure((LoggerContext) LoggerFactory.getILoggerFactory());
	}

	@Test
	void profileActive() throws Exception {
		this.environment.setActiveProfiles("production");
		initialize("production-profile.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void multipleNamesFirstProfileActive() throws Exception {
		this.environment.setActiveProfiles("production");
		initialize("multi-profile-names.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void multipleNamesSecondProfileActive() throws Exception {
		this.environment.setActiveProfiles("test");
		initialize("multi-profile-names.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void profileNotActive() throws Exception {
		initialize("production-profile.xml");
		this.logger.trace("Hello");
		assertThat(this.output).doesNotContain("Hello");
	}

	@Test
	void profileExpressionMatchFirst() throws Exception {
		this.environment.setActiveProfiles("production");
		initialize("profile-expression.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void profileExpressionMatchSecond() throws Exception {
		this.environment.setActiveProfiles("test");
		initialize("profile-expression.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void profileExpressionNoMatch() throws Exception {
		this.environment.setActiveProfiles("development");
		initialize("profile-expression.xml");
		this.logger.trace("Hello");
		assertThat(this.output).doesNotContain("Hello");
	}

	@Test
	void profileNestedActiveActive() throws Exception {
		doTestNestedProfile(true, "outer", "inner");
	}

	@Test
	void profileNestedActiveNotActive() throws Exception {
		doTestNestedProfile(false, "outer");
	}

	@Test
	void profileNestedNotActiveActive() throws Exception {
		doTestNestedProfile(false, "inner");
	}

	@Test
	void profileNestedNotActiveNotActive() throws Exception {
		doTestNestedProfile(false);
	}

	@Test
	void springProperty() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "my.example-property=test");
		initialize("property.xml");
		assertThat(this.context.getProperty("MINE")).isEqualTo("test");
	}

	@Test
	void relaxedSpringProperty() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, "my.EXAMPLE_PROPERTY=test");
		ConfigurationPropertySources.attach(this.environment);
		initialize("property.xml");
		assertThat(this.context.getProperty("MINE")).isEqualTo("test");
	}

	@Test
	void springPropertyNoValue() throws Exception {
		initialize("property.xml");
		assertThat(this.context.getProperty("SIMPLE")).isNull();
	}

	@Test
	void relaxedSpringPropertyNoValue() throws Exception {
		initialize("property.xml");
		assertThat(this.context.getProperty("MINE")).isNull();
	}

	@Test
	void springPropertyWithDefaultValue() throws Exception {
		initialize("property-default-value.xml");
		assertThat(this.context.getProperty("SIMPLE")).isEqualTo("foo");
	}

	@Test
	void relaxedSpringPropertyWithDefaultValue() throws Exception {
		initialize("property-default-value.xml");
		assertThat(this.context.getProperty("MINE")).isEqualTo("bar");
	}

	private void doTestNestedProfile(boolean expected, String... profiles) throws JoranException {
		this.environment.setActiveProfiles(profiles);
		initialize("nested.xml");
		this.logger.trace("Hello");
		if (expected) {
			assertThat(this.output).contains("Hello");
		}
		else {
			assertThat(this.output).doesNotContain("Hello");
		}

	}

	private void initialize(String config) throws JoranException {
		this.configurator.setContext(this.context);
		this.configurator.doConfigure(getClass().getResourceAsStream(config));
	}

}
