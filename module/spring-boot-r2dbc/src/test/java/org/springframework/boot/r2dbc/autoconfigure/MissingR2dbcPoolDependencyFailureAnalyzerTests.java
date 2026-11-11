/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MissingR2dbcPoolDependencyFailureAnalyzer}
 *
 * @author Andy Wilkinson
 */
class MissingR2dbcPoolDependencyFailureAnalyzerTests {

	private final MissingR2dbcPoolDependencyFailureAnalyzer failureAnalyzer = new MissingR2dbcPoolDependencyFailureAnalyzer();

	@Test
	void analyzeWhenDifferentFailureShouldReturnNull() {
		assertThat(this.failureAnalyzer.analyze(new Exception())).isNull();
	}

	@Test
	void analyzeWhenMissingR2dbcPoolDependencyShouldReturnAnalysis() {
		assertThat(this.failureAnalyzer.analyze(new MissingR2dbcPoolDependencyException())).isNotNull();
	}

}
