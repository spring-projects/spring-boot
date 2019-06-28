/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@link FlywayMigrationScriptMissingException}.
 *
 * @author Anand Shastri
 * @author Stephane Nicoll
 */
class FlywayMigrationScriptMissingFailureAnalyzer
		extends AbstractFailureAnalyzer<FlywayMigrationScriptMissingException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, FlywayMigrationScriptMissingException cause) {
		StringBuilder description = new StringBuilder("Flyway failed to initialize: ");
		if (cause.getLocations().isEmpty()) {
			return new FailureAnalysis(description.append("no migration scripts location is configured").toString(),
					"Check your Flyway configuration", cause);
		}
		description.append(String.format("none of the following migration scripts locations could be found:%n%n"));
		cause.getLocations().forEach((location) -> description.append(String.format("\t- %s%n", location)));
		return new FailureAnalysis(description.toString(),
				"Review the locations above or check your Flyway configuration", cause);
	}

}
