/*
 * Copyright 2012-2018 the original author or authors.
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

import io.micrometer.core.instrument.config.MissingRequiredConfigurationException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link MissingRequiredConfigurationException}.
 *
 * @author Andy Wilkinson
 */
class MissingRequiredConfigurationFailureAnalyzer
		extends AbstractFailureAnalyzer<MissingRequiredConfigurationException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			MissingRequiredConfigurationException cause) {
		StringBuilder description = new StringBuilder();
		description.append(cause.getMessage());
		if (!cause.getMessage().endsWith(".")) {
			description.append(".");
		}
		return new FailureAnalysis(description.toString(),
				"Update your application to provide the missing configuration.", cause);
	}

}
