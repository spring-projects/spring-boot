/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultipleConnectionPoolConfigurationsFailureAnalyzer}
 *
 * @author Andy Wilkinson
 */
class MultipleConnectionPoolConfigurationsFailureAnalyzerTests {

	private final MultipleConnectionPoolConfigurationsFailureAnalyzer failureAnalyzer = new MultipleConnectionPoolConfigurationsFailureAnalyzer();

	@Test
	void analyzeWhenDifferentFailureShouldReturnNull() {
		assertThat(this.failureAnalyzer.analyze(new Exception())).isNull();
	}

	@Test
	void analyzeWhenMultipleConnectionPoolConfigurationsShouldReturnAnalysis() {
		assertThat(this.failureAnalyzer.analyze(new MultipleConnectionPoolConfigurationsException())).isNotNull();
	}

}
