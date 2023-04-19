/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertySource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.logging.ConfigureClasspathToPreferLog4j2;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringProfileArbiter}.
 *
 * @author Phillip Webb
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathExclusions("logback-*.jar")
@ConfigureClasspathToPreferLog4j2
class SpringProfileArbiterTests {

	private CapturedOutput output;

	private final TestLog4J2LoggingSystem loggingSystem = new TestLog4J2LoggingSystem();

	private final MockEnvironment environment = new MockEnvironment();

	private final LoggingInitializationContext initializationContext = new LoggingInitializationContext(
			this.environment);

	private Logger logger;

	private Configuration configuration;

	@BeforeEach
	void setup(CapturedOutput output) {
		this.output = output;
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		this.configuration = loggerContext.getConfiguration();
		this.loggingSystem.cleanUp();
		this.logger = LogManager.getLogger(getClass());
		cleanUpPropertySources();
	}

	@AfterEach
	void cleanUp() {
		this.loggingSystem.cleanUp();
		LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
		loggerContext.stop();
		loggerContext.start(((Reconfigurable) this.configuration).reconfigure());
		cleanUpPropertySources();
	}

	@SuppressWarnings("unchecked")
	private void cleanUpPropertySources() { // https://issues.apache.org/jira/browse/LOG4J2-3618
		PropertiesUtil properties = PropertiesUtil.getProperties();
		Object environment = ReflectionTestUtils.getField(properties, "environment");
		Set<PropertySource> sources = (Set<PropertySource>) ReflectionTestUtils.getField(environment, "sources");
		sources.removeIf((candidate) -> candidate instanceof SpringEnvironmentPropertySource
				|| candidate instanceof SpringBootPropertySource);
	}

	@Test
	void profileActive() {
		this.environment.setActiveProfiles("production");
		initialize("production-profile.xml");
		this.logger.trace("Hello");
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

	private void initialize(String config) {
		this.environment.setProperty("logging.log4j2.config.override", getPackageResource(config));
		this.loggingSystem.initialize(this.initializationContext, null, null);
	}

	private String getPackageResource(String fileName) {
		String path = ClassUtils.getPackageName(getClass());
		return "src/test/resources/" + path.replace('.', '/') + "/" + fileName;
	}

}
