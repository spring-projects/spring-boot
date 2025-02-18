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

import org.springframework.boot.AotInitializerNotFoundException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that performs analysis of failures caused by a
 * {@link AotInitializerNotFoundException}.
 *
 * @author Moritz Halbritter
 */
class AotInitializerNotFoundFailureAnalyzer extends AbstractFailureAnalyzer<AotInitializerNotFoundException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, AotInitializerNotFoundException cause) {
		return new FailureAnalysis(cause.getMessage(), "Consider the following:\n"
				+ "\tDid you build the application with enabled AOT processing?\n"
				+ "\tIs the main class %s correct?\n".formatted(cause.getMainClass().getName())
				+ "\tIf you want to run the application in regular mode, remove the system property 'spring.aot.enabled'",
				cause);
	}

}
