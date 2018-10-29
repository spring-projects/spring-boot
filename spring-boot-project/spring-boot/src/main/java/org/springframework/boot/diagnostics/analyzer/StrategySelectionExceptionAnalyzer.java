/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.diagnostics.analyzer;

import org.hibernate.boot.registry.selector.spi.StrategySelectionException;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code StrategySelectionException}.
 *
 * @author Govinda Sakhare
 */
public class StrategySelectionExceptionAnalyzer
		extends AbstractFailureAnalyzer<StrategySelectionException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			StrategySelectionException strategySelectionException) {
		return new FailureAnalysis(
				"Dialect value is missing against database or incorrect value is provided!",
				"Set the value for the property spring.jpa.database-platform",
				rootFailure);
	}

}
