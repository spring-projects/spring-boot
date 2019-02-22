/*
 * Copyright 2012-2019 the original author or authors.
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

import org.apache.commons.lang3.StringUtils;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@code IllegalStateException}.
 *
 * @author Anand Shastri
 */
public class FlywayMigrationScriptMissingFailureAnalyzer
		extends AbstractFailureAnalyzer<IllegalStateException> {

	private static final String MISSING_SCRIPT_MESSAGE = "Cannot find migrations location in";

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, IllegalStateException cause) {
		if (cause.getMessage().startsWith(MISSING_SCRIPT_MESSAGE)) {
			String location = StringUtils.substringBetween(cause.getMessage(), "[", "]");
			return new FailureAnalysis("Cannot find migrations location in " + location,
					" please add migrations or check your Flyway configuration", cause);
		}
		return null;
	}

}
