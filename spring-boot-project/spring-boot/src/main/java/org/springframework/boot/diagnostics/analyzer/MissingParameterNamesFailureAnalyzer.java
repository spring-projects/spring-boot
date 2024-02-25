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

	static String ACTION = """
			Ensure that your compiler is configured to use the '-parameters' flag.
			You may need to update both your build tool settings as well as your IDE.
			(See https://github.com/spring-projects/spring-framework/wiki/Upgrading-to-Spring-Framework-6.x#parameter-name-retention)
							""";

	/**
     * Analyzes the given failure to determine if it is a missing parameter names failure.
     * 
     * @param failure the Throwable object representing the failure
     * @return a FailureAnalysis object containing the analysis result
     */
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

	/**
     * Analyzes the given throwable for missing parameters and returns a FailureAnalysis object if found.
     * 
     * @param rootFailure the root throwable
     * @param cause the cause throwable
     * @param seen a set of throwables that have already been analyzed to avoid infinite recursion
     * @return a FailureAnalysis object if missing parameters are found, null otherwise
     */
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

	/**
     * Checks if the given failure is a Spring Parameters Exception.
     * 
     * @param failure the Throwable object representing the failure
     * @return true if the failure is a Spring Parameters Exception, false otherwise
     */
    private static boolean isSpringParametersException(Throwable failure) {
		String message = failure.getMessage();
		return message != null && message.contains(USE_PARAMETERS_MESSAGE) && isSpringException(failure);
	}

	/**
     * Checks if the given Throwable is a Spring exception.
     * 
     * @param failure the Throwable to check
     * @return true if the Throwable is a Spring exception, false otherwise
     */
    private static boolean isSpringException(Throwable failure) {
		StackTraceElement[] elements = failure.getStackTrace();
		return elements.length > 0 && isSpringClass(elements[0].getClassName());
	}

	/**
     * Checks if the given class name belongs to the Spring framework.
     * 
     * @param className the name of the class to be checked
     * @return true if the class belongs to the Spring framework, false otherwise
     */
    private static boolean isSpringClass(String className) {
		return className != null && className.startsWith("org.springframework.");
	}

	/**
     * Returns a FailureAnalysis object containing the analysis of the given root failure and cause.
     *
     * @param rootFailure The root failure that occurred.
     * @param cause The cause of the failure.
     * @return A FailureAnalysis object containing the analysis.
     */
    private static FailureAnalysis getAnalysis(Throwable rootFailure, Throwable cause) {
		StringBuilder description = new StringBuilder(String.format("%s:%n", cause.getMessage()));
		if (rootFailure != cause) {
			description.append(String.format("%n    Resulting Failure: %s", getExceptionTypeAndMessage(rootFailure)));
		}
		return new FailureAnalysis(description.toString(), ACTION, rootFailure);
	}

	/**
     * Returns the exception type and message of the given Throwable object.
     * 
     * @param ex the Throwable object to get the exception type and message from
     * @return a String representing the exception type and message in the format "exceptionType: message"
     */
    private static String getExceptionTypeAndMessage(Throwable ex) {
		String message = ex.getMessage();
		return ex.getClass().getName() + (StringUtils.hasText(message) ? ": " + message : "");
	}

	/**
     * Appends a possibility to the given description.
     * 
     * @param description the StringBuilder object to append the possibility to
     */
    static void appendPossibility(StringBuilder description) {
		if (!description.toString().endsWith(System.lineSeparator())) {
			description.append("%n".formatted());
		}
		description.append("%n%s".formatted(POSSIBILITY));
	}

}
