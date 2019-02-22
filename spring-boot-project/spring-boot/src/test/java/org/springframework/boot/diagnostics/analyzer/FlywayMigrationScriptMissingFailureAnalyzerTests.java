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

import org.junit.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FlywayMigrationScriptMissingFailureAnalyzer}
 *
 * @author Anand Shastri
 */
public class FlywayMigrationScriptMissingFailureAnalyzerTests {

	private final FlywayMigrationScriptMissingFailureAnalyzer analyzer = new FlywayMigrationScriptMissingFailureAnalyzer();

	@Test
	public void analysisForFlywayScriptMissingFailureWhenIllegalStateExceptionIsForFlyway() {
		FailureAnalysis failureAnalysis = this.analyzer.analyze(new IllegalStateException(
				"Cannot find " + "migrations location in: [classpath:db/migration] "
						+ "(please add migrations or check your Flyway configuration)"));

		assertThat(failureAnalysis.getDescription())
				.endsWith("Cannot find migrations location in classpath:db/migration");
		assertThat(failureAnalysis.getAction())
				.endsWith(" please add migrations or check your Flyway configuration");
	}

	@Test
	public void analysisForFlywayScriptMissingFailureWhenIllegalStateExceptionIsNotForFlyway() {
		FailureAnalysis failureAnalysis = this.analyzer
				.analyze(new IllegalStateException("Some other exception"));

		assertThat(failureAnalysis).isNull();
	}

}
