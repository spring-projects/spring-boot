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

package org.springframework.boot.diagnostics.analyzer;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ValidationExceptionFailureAnalyzer}
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("hibernate-validator-*.jar")
public class ValidationExceptionFailureAnalyzerTests {

	@Test
	public void validatedPropertiesTest() {
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> new AnnotationConfigApplicationContext(TestConfiguration.class).close())
				.satisfies((ex) -> assertThat(new ValidationExceptionFailureAnalyzer().analyze(ex)).isNotNull());
	}

	@Test
	public void nonValidatedPropertiesTest() {
		new AnnotationConfigApplicationContext(NonValidatedTestConfiguration.class).close();
	}

	@EnableConfigurationProperties(TestProperties.class)
	static class TestConfiguration {

		TestConfiguration(TestProperties testProperties) {
		}

	}

	@ConfigurationProperties("test")
	@Validated
	private static class TestProperties {

	}

	@EnableConfigurationProperties(NonValidatedTestProperties.class)
	static class NonValidatedTestConfiguration {

		NonValidatedTestConfiguration(NonValidatedTestProperties testProperties) {
		}

	}

	@ConfigurationProperties("test")
	private static class NonValidatedTestProperties {

	}

}
