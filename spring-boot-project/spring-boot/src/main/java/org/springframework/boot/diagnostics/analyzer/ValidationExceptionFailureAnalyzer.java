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

import javax.validation.ValidationException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * A {@link FailureAnalyzer} that performs analysis of failures caused by a
 * {@link ValidationException}.
 *
 * @author Andy Wilkinson
 */
class ValidationExceptionFailureAnalyzer extends AbstractFailureAnalyzer<ValidationException> {

	private static final String JAVAX_MISSING_IMPLEMENTATION_MESSAGE = "Unable to create a "
			+ "Configuration, because no Bean Validation provider could be found";

	private static final String JAKARTA_MISSING_IMPLEMENTATION_MESSAGE = "Unable to create a "
			+ "Configuration, because no Jakarta Bean Validation provider could be found";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ValidationException cause) {
		if (cause.getMessage().startsWith(JAVAX_MISSING_IMPLEMENTATION_MESSAGE)
				|| cause.getMessage().startsWith(JAKARTA_MISSING_IMPLEMENTATION_MESSAGE)) {
			return new FailureAnalysis(
					"The Bean Validation API is on the classpath but no implementation could be found",
					"Add an implementation, such as Hibernate Validator, to the classpath", cause);
		}
		return null;
	}

}
