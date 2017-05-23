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

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link BeanCreationFailureAnalyzer}.
 *
 * @author Stephane Nicoll
 */
public class BeanCreationFailureAnalyzerTests {

	private final FailureAnalyzer analyzer = new BeanCreationFailureAnalyzer();

	@Test
	public void failureWithDefinition() {
		FailureAnalysis analysis = performAnalysis(
				BeanCreationFailureConfiguration.class);
		assertThat(analysis.getDescription()).contains("bean named 'foo'",
				"Property bar is not set",
				"defined in " + BeanCreationFailureConfiguration.class.getName());
		assertThat(analysis.getDescription()).doesNotContain("Failed to instantiate");
		assertThat(analysis.getAction()).isNull();
	}

	private FailureAnalysis performAnalysis(Class<?> configuration) {
		FailureAnalysis analysis = this.analyzer.analyze(createFailure(configuration));
		assertThat(analysis).isNotNull();
		return analysis;
	}

	private Exception createFailure(Class<?> configuration) {
		try (ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(
				configuration)) {
			fail("Expected failure did not occur");
			return null;
		}
		catch (Exception ex) {
			return ex;
		}
	}

	@Configuration
	static class BeanCreationFailureConfiguration {

		@Bean
		public String foo() {
			throw new IllegalStateException("Property bar is not set");
		}

	}

}
