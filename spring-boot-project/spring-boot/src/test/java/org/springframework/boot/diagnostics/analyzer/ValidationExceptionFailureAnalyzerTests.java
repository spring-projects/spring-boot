/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link ValidationExceptionFailureAnalyzer}
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("hibernate-validator-*.jar")
public class ValidationExceptionFailureAnalyzerTests {

	@Test
	public void test() throws Exception {
		try {
			new AnnotationConfigApplicationContext(TestConfiguration.class).close();
			fail("Expected failure did not occur");
		}
		catch (Exception ex) {
			FailureAnalysis analysis = new ValidationExceptionFailureAnalyzer()
					.analyze(ex);
			assertThat(analysis).isNotNull();
		}
	}

	@EnableConfigurationProperties(TestProperties.class)
	static class TestConfiguration {

		TestConfiguration(TestProperties testProperties) {

		}

	}

	@ConfigurationProperties("test")
	private static class TestProperties {

	}

}
