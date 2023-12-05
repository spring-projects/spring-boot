/*
 * Copyright 2012-2023 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Phillip Webb
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class MissingParametersFailureAnalyzer implements FailureAnalyzer {

	private static final String USE_PARAMETERS_MESSAGE = "Ensure that the compiler uses the '-parameters' flag";

	@Override
	public FailureAnalysis analyze(Throwable failure) {
		return analyze(failure, failure, new HashSet<>());
	}

	private FailureAnalysis analyze(Throwable rootFailure, Throwable cause, Set<Throwable> seen) {
		if (cause == null || !seen.add(cause)) {
			return null;
		}
		if (isSpringParametersException(cause)) {
			return getAnalysis(rootFailure, cause);
		}
		FailureAnalysis analysis = analyze(rootFailure, cause.getCause(), seen);
		if (analysis != null) {
			return analysis;
		}
		for (Throwable suppressed : cause.getSuppressed()) {
			analysis = analyze(rootFailure, suppressed, seen);
			if (analysis != null) {
				return analysis;
			}
		}
		return null;
	}

	private boolean isSpringParametersException(Throwable failure) {
		String message = failure.getMessage();
		return message != null && message.contains(USE_PARAMETERS_MESSAGE) && isSpringException(failure);
	}

	private boolean isSpringException(Throwable failure) {
		StackTraceElement[] elements = failure.getStackTrace();
		return elements.length > 0 && isSpringClass(elements[0].getClassName());
	}

	private boolean isSpringClass(String className) {
		return className != null && className.startsWith("org.springframework.");
	}

	private FailureAnalysis getAnalysis(Throwable rootFailure, Throwable cause) {
		String description = "";
		if (rootFailure != cause) {

		}
		String action = "";
		return new FailureAnalysis(description, action, rootFailure);
	}

}
