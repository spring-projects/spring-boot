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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.config.validate.Validated.Invalid;
import io.micrometer.core.instrument.config.validate.ValidationException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link ValidationException}.
 *
 * @author Andy Wilkinson
 */
class ValidationFailureAnalyzer extends AbstractFailureAnalyzer<ValidationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ValidationException cause) {
		StringBuilder description = new StringBuilder(String.format("Invalid Micrometer configuration detected:%n"));
		for (Invalid<?> failure : cause.getValidation().failures()) {
			description.append(String.format("%n  - %s was '%s' but it %s", failure.getProperty(), failure.getValue(),
					failure.getMessage()));
		}
		return new FailureAnalysis(description.toString(),
				"Update your application to correct the invalid configuration.", cause);
	}

}
