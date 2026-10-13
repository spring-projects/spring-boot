/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.AotInitializerNotFoundException;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AotInitializerNotFoundFailureAnalyzer}.
 *
 * @author Moritz Halbritter
 */
class AotInitializerNotFoundFailureAnalyzerTests {

	@Test
	void shouldAnalyze() {
		FailureAnalysis analysis = analyze();
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).isEqualTo(
				"Startup with AOT mode enabled failed: AOT initializer class org.springframework.boot.diagnostics.analyzer.AotInitializerNotFoundFailureAnalyzerTests__ApplicationContextInitializer could not be found");
		assertThat(analysis.getAction()).isEqualTo(
				"""
						Consider the following:
						\tDid you build the application with enabled AOT processing?
						\tIs the main class org.springframework.boot.diagnostics.analyzer.AotInitializerNotFoundFailureAnalyzerTests correct?
						\tIf you want to run the application in regular mode, remove the system property 'spring.aot.enabled'""");
	}

	private @Nullable FailureAnalysis analyze() {
		return new AotInitializerNotFoundFailureAnalyzer()
			.analyze(new AotInitializerNotFoundException(AotInitializerNotFoundFailureAnalyzerTests.class,
					AotInitializerNotFoundFailureAnalyzerTests.class + "__ApplicationContextInitializer"));
	}

}
