/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IncompatibleConfigurationFailureAnalyzer}
 *
 * @author Brian Clozel
 */
class IncompatibleConfigurationFailureAnalyzerTests {

	@Test
	void incompatibleConfigurationListsKeys() {
		FailureAnalysis failureAnalysis = performAnalysis("spring.first.key", "spring.second.key");
		assertThat(failureAnalysis).isNotNull();
		assertThat(failureAnalysis.getDescription()).contains(
				"The following configuration properties have incompatible values: [spring.first.key, spring.second.key]");
		assertThat(failureAnalysis.getAction())
			.contains("Review the docs for spring.first.key, spring.second.key and change the configured values.");
	}

	private @Nullable FailureAnalysis performAnalysis(String... keys) {
		IncompatibleConfigurationException failure = new IncompatibleConfigurationException(keys);
		return new IncompatibleConfigurationFailureAnalyzer().analyze(failure);
	}

}
