/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.lifecycle;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerEnvironmentNotFoundFailureAnalyzer}.
 *
 * @author Dmytro Nosan
 */
class DockerEnvironmentNotFoundFailureAnalyzerTests {

	private final DockerEnvironmentNotFoundFailureAnalyzer analyzer = new DockerEnvironmentNotFoundFailureAnalyzer();

	@Test
	void shouldReturnFailureAnalysisWhenMessageMatches() {
		IllegalStateException cause = new IllegalStateException(
				"Could not find a valid Docker environment. Please see logs and check configuration");
		FailureAnalysis analysis = this.analyzer
			.analyze(new RuntimeException("Root", new RuntimeException("Intermediate", cause)));
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription())
			.isEqualTo("Could not find a valid Docker environment for Testcontainers.");
		assertThat(analysis.getAction())
			.contains("Ensure a Docker-compatible container engine is installed and running");
		assertThat(analysis.getCause()).isSameAs(cause);
	}

	@Test
	void shouldReturnNullWhenMessageDoesNotMatch() {
		FailureAnalysis analysis = this.analyzer.analyze(new IllegalStateException("Some message"));
		assertThat(analysis).isNull();
	}

	@Test
	void shouldReturnNullWhenMessageIsNull() {
		FailureAnalysis analysis = this.analyzer.analyze(new IllegalStateException());
		assertThat(analysis).isNull();
	}

}
