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

package org.springframework.boot.jpa.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BootstrapExecutorRequiredFailureAnalyzer}.
 *
 * @author Phillip Webb
 */
class BootstrapExecutorRequiredFailureAnalyzerTests {

	private final BootstrapExecutorRequiredFailureAnalyzer analyzer = new BootstrapExecutorRequiredFailureAnalyzer();

	@Test
	void analyzeWhenBootstrapExecutorRequiredExceptionWithProperties() {
		BootstrapExecutorRequiredException exception = BootstrapExecutorRequiredException.ofProperty("testname",
				"testvalue");
		FailureAnalysis result = this.analyzer.analyze(exception);
		assertThat(result).isNotNull();
		assertThat(result.getDescription()).isEqualTo(
				"An EntityManagerFactoryBean bootstrap executor is required when 'testname' is set to 'testvalue'");
		assertThat(result.getAction()).isEqualTo(
				"""
						Use a different 'testname' value or provide a bootstrap executor using one of the following methods:
							- With an auto-configured task executor (you may need to set 'spring.task.execution.mode' to 'force').
							- With an AsyncTaskExecutor bean named 'applicationTaskExecutor'.
							- Using a EntityManagerFactoryBuilderCustomizer.
							""");
	}

	@Test
	void analyzeWhenBootstrapExecutorRequiredExceptionWithMessage() {
		BootstrapExecutorRequiredException exception = new BootstrapExecutorRequiredException("A custom message");
		FailureAnalysis result = this.analyzer.analyze(exception);
		assertThat(result).isNotNull();
		assertThat(result.getDescription()).isEqualTo("A custom message");
		assertThat(result.getAction()).isEqualTo(
				"""
						Provide a bootstrap executor using one of the following methods:
							- With an auto-configured task executor (you may need to set 'spring.task.execution.mode' to 'force').
							- With an AsyncTaskExecutor bean named 'applicationTaskExecutor'.
							- Using a EntityManagerFactoryBuilderCustomizer.
							""");
	}

}
