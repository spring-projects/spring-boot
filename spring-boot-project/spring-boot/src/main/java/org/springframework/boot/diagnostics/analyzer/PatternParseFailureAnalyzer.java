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
		return new FailureAnalysis("Invalid mapping pattern detected: " + cause.toDetailedString(),
				"Fix this pattern in your application or switch to the legacy parser implementation with "
						+ "'spring.mvc.pathpattern.matching-strategy=ant_path_matcher'.",
				cause);
	}

}
