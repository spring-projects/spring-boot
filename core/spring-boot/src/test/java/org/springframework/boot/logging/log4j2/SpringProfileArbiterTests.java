/*
 * Copyright 2012-present the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertySource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.testsupport.logging.ConfigureClasspathToPreferLog4j2;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

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

	private TestLog4J2LoggingSystem loggingSystem;

	private final MockEnvironment environment = new MockEnvironment();

	private final LoggingInitializationContext initializationContext = new LoggingInitializationContext(
			this.environment);

	private Logger logger;

	@BeforeEach
	void setup(CapturedOutput output, TestInfo testInfo) {
		this.output = output;
		this.loggingSystem = new TestLog4J2LoggingSystem(testInfo.getDisplayName());
		this.logger = this.loggingSystem.getLoggerContext().getLogger(getClass().getName());
		cleanUpPropertySources();
	}

	@AfterEach
	void cleanUp() {
		this.loggingSystem.cleanUp();
		cleanUpPropertySources();
	}

	@SuppressWarnings("unchecked")
	private void cleanUpPropertySources() { // https://issues.apache.org/jira/browse/LOG4J2-3618
		PropertiesUtil properties = PropertiesUtil.getProperties();
		Object environment = ReflectionTestUtils.getField(properties, "environment");
		assertThat(environment).isNotNull();
		Set<PropertySource> sources = (Set<PropertySource>) ReflectionTestUtils.getField(environment, "sources");
		assertThat(sources).isNotNull();
		sources.removeIf((candidate) -> candidate instanceof SpringEnvironmentPropertySource
				|| candidate instanceof SpringBootPropertySource);
	}

	@Test
	@WithProductionProfileXmlResource
	void profileActive() {
		this.environment.setActiveProfiles("production");
		initialize("production-profile.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	@WithMultiProfileNamesXmlResource
	void multipleNamesFirstProfileActive() {
		this.environment.setActiveProfiles("production");
		initialize("multi-profile-names.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	@WithMultiProfileNamesXmlResource
	void multipleNamesSecondProfileActive() {
		this.environment.setActiveProfiles("test");
		initialize("multi-profile-names.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	@WithProductionProfileXmlResource
	void profileNotActive() {
		initialize("production-profile.xml");
		this.logger.trace("Hello");
		assertThat(this.output).doesNotContain("Hello");
	}

	@Test
	@WithProfileExpressionXmlResource
	void profileExpressionMatchFirst() {
		this.environment.setActiveProfiles("production");
		initialize("profile-expression.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	@WithProfileExpressionXmlResource
	void profileExpressionMatchSecond() {
		this.environment.setActiveProfiles("test");
		initialize("profile-expression.xml");
		this.logger.trace("Hello");
		assertThat(this.output).contains("Hello");
	}

	@Test
	@WithProfileExpressionXmlResource
	void profileExpressionNoMatch() {
		this.environment.setActiveProfiles("development");
		initialize("profile-expression.xml");
		this.logger.trace("Hello");
		assertThat(this.output).doesNotContain("Hello");
	}

	private void initialize(String config) {
		this.environment.setProperty("logging.log4j2.config.override", "classpath:" + config);
		this.loggingSystem.initialize(this.initializationContext, null, null);
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "multi-profile-names.xml", content = """
			<?xml version="1.0" encoding="UTF-8"?>
			<Configuration>
			    <Loggers>
				    <SpringProfile name="production, test">
						<Logger name="org.springframework.boot.logging.log4j2" level="TRACE" />
					</SpringProfile>
				</Loggers>
			</Configuration>
			""")
	private @interface WithMultiProfileNamesXmlResource {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "profile-expression.xml", content = """
			<?xml version="1.0" encoding="UTF-8"?>
			<Configuration>
			    <Loggers>
				    <SpringProfile name="production | test">
						<Logger name="org.springframework.boot.logging.log4j2" level="TRACE" />
					</SpringProfile>
				</Loggers>
			</Configuration>
			""")
	private @interface WithProfileExpressionXmlResource {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "production-profile.xml", content = """
			<?xml version="1.0" encoding="UTF-8"?>
			<Configuration>
			    <Loggers>
				    <SpringProfile name="production">
						<Logger name="org.springframework.boot.logging.log4j2" level="TRACE" />
					</SpringProfile>
				</Loggers>
			</Configuration>
			""")
	private @interface WithProductionProfileXmlResource {

	}

}
