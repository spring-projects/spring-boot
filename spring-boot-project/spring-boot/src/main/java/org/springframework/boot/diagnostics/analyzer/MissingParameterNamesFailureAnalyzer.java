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

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;

/**
 * {@link FailureAnalyzer} for exceptions caused by missing parameter names. This analyzer
 * is ordered last, if other analyzers wish to also report parameter actions they can use
 * the {@link #analyzeForMissingParameters(Throwable)} static method.
 *
 * @author Phillip Webb
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class MissingParameterNamesFailureAnalyzer implements FailureAnalyzer {

	private static final String USE_PARAMETERS_MESSAGE = "Ensure that the compiler uses the '-parameters' flag";

	static final String POSSIBILITY = "This may be due to missing parameter name information";

	static final String ACTION = """
			Ensure that your compiler is configured to use the '-parameters' flag.
			You may need to update both your build tool settings as well as your IDE.
			(See https://github.com/spring-projects/spring-framework/wiki/Upgrading-to-Spring-Framework-6.x#parameter-name-retention)
							""";

	@Override
	public FailureAnalysis analyze(Throwable failure) {
		return analyzeForMissingParameters(failure);
	}

	/**
	 * Analyze the given failure for missing parameter name exceptions.
	 * @param failure the failure to analyze
	 * @return a failure analysis or {@code null}
	 */
	static FailureAnalysis analyzeForMissingParameters(Throwable failure) {
		return analyzeForMissingParameters(failure, failure, new HashSet<>());
	}

	private static FailureAnalysis analyzeForMissingParameters(Throwable rootFailure, Throwable cause,
			Set<Throwable> seen) {
		if (cause != null && seen.add(cause)) {
			if (isSpringParametersException(cause)) {
				return getAnalysis(rootFailure, cause);
			}
			FailureAnalysis analysis = analyzeForMissingParameters(rootFailure, cause.getCause(), seen);
			if (analysis != null) {
				return analysis;
			}
			for (Throwable suppressed : cause.getSuppressed()) {
				analysis = analyzeForMissingParameters(rootFailure, suppressed, seen);
				if (analysis != null) {
					return analysis;
				}
			}
		}
		return null;
	}

	private static boolean isSpringParametersException(Throwable failure) {
		String message = failure.getMessage();
		return message != null && message.contains(USE_PARAMETERS_MESSAGE) && isSpringException(failure);
	}

	private static boolean isSpringException(Throwable failure) {
		StackTraceElement[] elements = failure.getStackTrace();
		return elements.length > 0 && isSpringClass(elements[0].getClassName());
	}

	private static boolean isSpringClass(String className) {
		return className != null && className.startsWith("org.springframework.");
	}

	private static FailureAnalysis getAnalysis(Throwable rootFailure, Throwable cause) {
		StringBuilder description = new StringBuilder(String.format("%s:%n", cause.getMessage()));
		if (rootFailure != cause) {
			description.append(String.format("%n    Resulting Failure: %s", getExceptionTypeAndMessage(rootFailure)));
		}
		return new FailureAnalysis(description.toString(), ACTION, rootFailure);
	}

	private static String getExceptionTypeAndMessage(Throwable ex) {
		String message = ex.getMessage();
		return ex.getClass().getName() + (StringUtils.hasText(message) ? ": " + message : "");
	}

	static void appendPossibility(StringBuilder description) {
		if (!description.toString().endsWith(System.lineSeparator())) {
			description.append("%n".formatted());
		}
		description.append("%n%s".formatted(POSSIBILITY));
	}

}
