/*
 * Copyright 2012-present the original author or authors.
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
