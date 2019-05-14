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

package org.springframework.boot.autoconfigure.liquibase;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@code FailureAnalyzer} that performs analysis of failures caused by a
 * {@link LiquibaseDataSourceMissingException}.
 *
 * @author Ilya Lukyanovich
 */
class LiquibaseDataSourceMissingFailureAnalyzer
		extends AbstractFailureAnalyzer<LiquibaseDataSourceMissingException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			LiquibaseDataSourceMissingException cause) {
		return new FailureAnalysis("No DataSource found for liquibsae",
				"Please provide a javax.sql.DataSource bean or liquibase datasource configuration",
				cause);
	}

}
