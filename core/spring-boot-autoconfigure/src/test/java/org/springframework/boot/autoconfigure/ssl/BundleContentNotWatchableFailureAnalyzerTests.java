/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.ssl;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BundleContentNotWatchableFailureAnalyzer}.
 *
 * @author Moritz Halbritter
 */
class BundleContentNotWatchableFailureAnalyzerTests {

	@Test
	void shouldAnalyze() {
		FailureAnalysis failureAnalysis = performAnalysis(null);
		assertThat(failureAnalysis).isNotNull();
		assertThat(failureAnalysis.getDescription()).isEqualTo(
				"The content of 'name' is not watchable. Only 'file:' resources are watchable, but 'classpath:resource.pem' has been set");
		assertThat(failureAnalysis.getAction())
			.isEqualTo("Update your application to correct the invalid configuration:\n"
					+ "Either use a watchable resource, or disable bundle reloading by setting reload-on-update = false on the bundle.");
	}

	@Test
	void shouldAnalyzeWithBundle() {
		FailureAnalysis failureAnalysis = performAnalysis("bundle-1");
		assertThat(failureAnalysis).isNotNull();
		assertThat(failureAnalysis.getDescription()).isEqualTo(
				"The content of 'name' from bundle 'bundle-1' is not watchable'. Only 'file:' resources are watchable, but 'classpath:resource.pem' has been set");
	}

	private @Nullable FailureAnalysis performAnalysis(@Nullable String bundle) {
		BundleContentNotWatchableException failure = new BundleContentNotWatchableException(
				new BundleContentProperty("name", "classpath:resource.pem"));
		if (bundle != null) {
			failure = failure.withBundleName(bundle);
		}
		return new BundleContentNotWatchableFailureAnalyzer().analyze(failure);
	}

}
