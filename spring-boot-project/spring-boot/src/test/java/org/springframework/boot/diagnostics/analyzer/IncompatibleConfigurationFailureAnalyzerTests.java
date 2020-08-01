/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.IncompatibleConfigurationException;
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
		assertThat(failureAnalysis.getDescription()).contains(
				"The following configuration properties have incompatible values: [spring.first.key, spring.second.key]");
		assertThat(failureAnalysis.getAction())
				.contains("Review the docs for spring.first.key, spring.second.key and change the configured values.");
	}

	private FailureAnalysis performAnalysis(String... keys) {
		IncompatibleConfigurationException failure = new IncompatibleConfigurationException(keys);
		return new IncompatibleConfigurationFailureAnalyzer().analyze(failure);
	}

}
