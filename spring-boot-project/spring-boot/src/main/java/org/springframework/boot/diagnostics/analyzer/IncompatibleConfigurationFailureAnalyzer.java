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

import java.util.stream.Collectors;

import org.springframework.boot.context.properties.IncompatibleConfigurationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code IncompatibleConfigurationException}.
 *
 * @author Brian Clozel
 */
class IncompatibleConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<IncompatibleConfigurationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, IncompatibleConfigurationException cause) {
		String action = String.format("Review the docs for %s and change the configured values.",
				cause.getIncompatibleKeys().stream().collect(Collectors.joining(", ")));
		return new FailureAnalysis(cause.getMessage(), action, cause);
	}

}
