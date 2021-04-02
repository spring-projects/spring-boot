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

package org.springframework.boot.logging.log4j2;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootXmlConfiguration}.
 *
 * @author : Kong Wu
 */
@ExtendWith(OutputCaptureExtension.class)
public class SpringBootXmlConfigurationTests {

	private MockEnvironment environment;

	private LoggingInitializationContext initializationContext;

	private Logger logger;

	private CapturedOutput output;

	private Configuration configuration;

	@BeforeEach
	void setup(CapturedOutput output) {
		this.output = output;
		this.environment = new MockEnvironment();
		this.initializationContext = new LoggingInitializationContext(this.environment);
	}

	@AfterEach
	void reset() {
		this.configuration.stop();
	}

	@Test
	void profileActive() {
		this.environment.setActiveProfiles("production");
		initialize("production-profile.xml");
		this.logger.error("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void multipleNamesFirstProfileActive() {
		this.environment.setActiveProfiles("production");
		initialize("multi-profile-names.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void multipleNamesSecondProfileActive() {
		this.environment.setActiveProfiles("test");
		initialize("multi-profile-names.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void profileNotActive() {
		initialize("production-profile.xml");
		this.logger.trace("Hello");
		assertThat(this.output).doesNotContain("Hello");
	}

	@Test
	void profileExpressionMatchFirst() {
		this.environment.setActiveProfiles("production");
		initialize("profile-expression.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void profileExpressionMatchSecond() {
		this.environment.setActiveProfiles("test");
		initialize("profile-expression.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	void profileExpressionNoMatch() {
		this.environment.setActiveProfiles("development");
		initialize("profile-expression.xml");
		this.logger.trace("Hello");
		assertThat(this.output).doesNotContain("Hello");
	}

	@Test
	void profileNestedActiveActive() {
		doTestNestedProfile(true, "outer", "inner");
	}

	@Test
	void profileNestedActiveNotActive() {
		doTestNestedProfile(false, "outer");
	}

	@Test
	void profileNestedNotActiveActive() {
		doTestNestedProfile(false, "inner");
	}

	@Test
	void profileNestedNotActiveNotActive() {
		doTestNestedProfile(false);
	}

	private void doTestNestedProfile(boolean expected, String... profiles) {
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

	private void initialize(String config) {
		LoggerContext context = new LoggerContext("SpringBoot");
		this.configuration = new SpringBootXmlConfiguration(this.initializationContext, context,
				configurationSource(config));
		this.configuration.initialize();
		context.start(this.configuration);
		this.logger = context.getLogger(this.getClass());
	}

	private ConfigurationSource configurationSource(String config) {
		try (InputStream in = getClass().getResourceAsStream(config)) {
			return new ConfigurationSource(in);
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
