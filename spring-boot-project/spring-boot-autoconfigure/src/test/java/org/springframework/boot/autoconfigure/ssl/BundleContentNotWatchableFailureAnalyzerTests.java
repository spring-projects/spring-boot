/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.ssl;

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
		assertThat(failureAnalysis.getDescription()).isEqualTo(
				"The content of 'name' is not watchable. Only 'file:' resources are watchable, but 'classpath:resource.pem' has been set");
		assertThat(failureAnalysis.getAction())
			.isEqualTo("Update your application to correct the invalid configuration:\n"
					+ "Either use a watchable resource, or disable bundle reloading by setting reload-on-update = false on the bundle.");
	}

	@Test
	void shouldAnalyzeWithBundle() {
		FailureAnalysis failureAnalysis = performAnalysis("bundle-1");
		assertThat(failureAnalysis.getDescription()).isEqualTo(
				"The content of 'name' from bundle 'bundle-1' is not watchable'. Only 'file:' resources are watchable, but 'classpath:resource.pem' has been set");
	}

	private FailureAnalysis performAnalysis(String bundle) {
		BundleContentNotWatchableException failure = new BundleContentNotWatchableException(
				new BundleContentProperty("name", "classpath:resource.pem"));
		if (bundle != null) {
			failure = failure.withBundleName(bundle);
		}
		return new BundleContentNotWatchableFailureAnalyzer().analyze(failure);
	}

}
