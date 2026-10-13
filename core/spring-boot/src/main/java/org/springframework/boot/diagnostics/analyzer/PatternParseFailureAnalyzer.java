/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics.analyzer;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.web.util.pattern.PatternParseException;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code PatternParseException}.
 *
 * @author Brian Clozel
 */
class PatternParseFailureAnalyzer extends AbstractFailureAnalyzer<PatternParseException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, PatternParseException cause) {
		return new FailureAnalysis("Invalid mapping pattern detected:\n" + cause.toDetailedString(),
				"Fix this pattern in your application or switch to the legacy parser implementation with "
						+ "'spring.mvc.pathmatch.matching-strategy=ant_path_matcher'.",
				cause);
	}

}
