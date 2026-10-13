/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

/**
 * Tests for {@link ValidationExceptionFailureAnalyzer}
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("hibernate-validator-*.jar")
class JakartaApiValidationExceptionFailureAnalyzerTests {

	@Test
	void validatedPropertiesTest() {
		assertThatException().isThrownBy(() -> new AnnotationConfigApplicationContext(TestConfiguration.class).close())
			.satisfies((ex) -> assertThat(new ValidationExceptionFailureAnalyzer().analyze(ex)).isNotNull());
	}

	@Test
	void nonValidatedPropertiesTest() {
		new AnnotationConfigApplicationContext(NonValidatedTestConfiguration.class).close();
	}

	@EnableConfigurationProperties(TestProperties.class)
	static class TestConfiguration {

		TestConfiguration(TestProperties testProperties) {
		}

	}

	@ConfigurationProperties("test")
	@Validated
	static class TestProperties {

	}

	@EnableConfigurationProperties(NonValidatedTestProperties.class)
	static class NonValidatedTestConfiguration {

		NonValidatedTestConfiguration(NonValidatedTestProperties testProperties) {
		}

	}

	@ConfigurationProperties("test")
	static class NonValidatedTestProperties {

	}

}
