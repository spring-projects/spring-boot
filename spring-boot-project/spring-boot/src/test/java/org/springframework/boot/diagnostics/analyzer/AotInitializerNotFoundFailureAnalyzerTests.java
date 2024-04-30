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

package org.springframework.boot.diagnostics.analyzer;

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
		assertThat(analysis.getDescription()).isEqualTo(
				"Startup with AOT mode enabled failed: AOT initializer class org.springframework.boot.diagnostics.analyzer.AotInitializerNotFoundFailureAnalyzerTests__ApplicationContextInitializer could not be found");
		assertThat(analysis.getAction()).isEqualTo(
				"""
						Consider the following:
						\tDid you build the application with enabled AOT processing?
						\tIs the main class org.springframework.boot.diagnostics.analyzer.AotInitializerNotFoundFailureAnalyzerTests correct?
						\tIf you want to run the application in regular mode, remove the system property 'spring.aot.enabled'""");
	}

	private FailureAnalysis analyze() {
		return new AotInitializerNotFoundFailureAnalyzer()
			.analyze(new AotInitializerNotFoundException(AotInitializerNotFoundFailureAnalyzerTests.class,
					AotInitializerNotFoundFailureAnalyzerTests.class + "__ApplicationContextInitializer"));
	}

}
