/*
 * Copyright 2012-present the original author or authors.
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

	private final PathPatternParser parser = new PathPatternParser();

	@Test
	void patternParseFailureQuotesPattern() {
		FailureAnalysis failureAnalysis = performAnalysis("/spring/**/framework");
		assertThat(failureAnalysis.getDescription())
			.contains("Invalid mapping pattern detected:\n" + "/spring/**/framework\n" + "        ^");
		assertThat(failureAnalysis.getAction())
			.contains("Fix this pattern in your application or switch to the legacy parser"
					+ " implementation with 'spring.mvc.pathmatch.matching-strategy=ant_path_matcher'.");
	}

	private FailureAnalysis performAnalysis(String pattern) {
		PatternParseException failure = createFailure(pattern);
		FailureAnalysis analyze = new PatternParseFailureAnalyzer().analyze(failure);
		assertThat(analyze).isNotNull();
		return analyze;
	}

	PatternParseException createFailure(String pattern) {
		try {
			this.parser.parse(pattern);
			throw new AssertionError("Should not be reached");
		}
		catch (PatternParseException ex) {
			return ex;
		}
	}

}
