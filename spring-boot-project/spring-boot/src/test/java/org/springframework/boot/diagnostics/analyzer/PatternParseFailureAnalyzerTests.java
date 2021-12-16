/*
 * Copyright 2012-2021 the original author or authors.
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

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PatternParseFailureAnalyzer}
 *
 * @author Brian Clozel
 */
class PatternParseFailureAnalyzerTests {

	private PathPatternParser parser = new PathPatternParser();

	@Test
	void patternParseFailureQuotesPattern() {
		FailureAnalysis failureAnalysis = performAnalysis("/spring/**/framework");
		assertThat(failureAnalysis.getDescription()).contains("Invalid mapping pattern detected: /spring/**/framework");
		assertThat(failureAnalysis.getAction())
				.contains("Fix this pattern in your application or switch to the legacy parser"
						+ " implementation with 'spring.mvc.pathpattern.matching-strategy=ant_path_matcher'.");
	}

	private FailureAnalysis performAnalysis(String pattern) {
		PatternParseException failure = createFailure(pattern);
		assertThat(failure).isNotNull();
		return new PatternParseFailureAnalyzer().analyze(failure);
	}

	PatternParseException createFailure(String pattern) {
		try {
			this.parser.parse(pattern);
			return null;
		}
		catch (PatternParseException ex) {
			return ex;
		}
	}

}
