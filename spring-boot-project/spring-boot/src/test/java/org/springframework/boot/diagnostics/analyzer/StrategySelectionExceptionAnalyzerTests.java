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
import org.junit.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StrategySelectionExceptionAnalyzer}.
 *
 * @author Govinda Sakhare
 */
public class StrategySelectionExceptionAnalyzerTests {

	private StrategySelectionExceptionAnalyzer analyzer = new StrategySelectionExceptionAnalyzer();

	@Test
	public void dialectValidPropertyIsSetTest() {
		StrategySelectionException failure = createFailure(StrategySelectionExceptionAnalyzerTests.ValidDialectConfiguration.class);
		FailureAnalysis analysis = this.analyzer.analyze(failure);
		assertThat(analysis).isNull();
	}

	@Test
	public void dialectInvalidPropertyIsSetTest() {
		StrategySelectionException failure = createFailure(StrategySelectionExceptionAnalyzerTests.InvalidDialectConfiguration.class);
		FailureAnalysis analysis = this.analyzer.analyze(failure);
		System.out.println("$$$ => "+ analysis.getDescription());
		assertThat(analysis.getDescription()).contains("Dialect value is missing against database or value is provided!");
	}

	@Test
	public void noDialectPropertyIsSetTest() {
		StrategySelectionException failure = createFailure(StrategySelectionExceptionAnalyzerTests.NoDialect.class);
		FailureAnalysis analysis = this.analyzer.analyze(failure);
		assertThat(analysis.getDescription()).contains("Dialect value is missing against database or incorrect value is provided!");
	}

	private StrategySelectionException createFailure(Class<?> configuration) {
		try {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			context.register(configuration);
			context.refresh();
			context.close();
			return null;
		}
		catch (StrategySelectionException ex) {
			return ex;
		}
	}


	@ConfigurationProperties("spring.jpa")
	private static class NoDialect {

		String database = "postgresql";

		public String getDatabase() {
			return this.database;
		}

		public void setDatabase(String database) {
			this.database = database;
		}

	}

	@EnableConfigurationProperties(StrategySelectionExceptionAnalyzerTests.NoDialect.class)
	static class NoDialectConfiguration {

		NoDialectConfiguration(
				StrategySelectionExceptionAnalyzerTests.NoDialect testProperties) {
		}

	}

	@ConfigurationProperties("spring.jpa")
	private static class InvalidDialect {

		String databasePlatform = "incorrect_platform";

		public String getDatabasePlatform() {
			return this.databasePlatform;
		}

		public void setDatabasePlatform(String databasePlatform) {
			this.databasePlatform = databasePlatform;
		}

	}

	@EnableConfigurationProperties(StrategySelectionExceptionAnalyzerTests.InvalidDialect.class)
	static class InvalidDialectConfiguration {

		InvalidDialectConfiguration(
				StrategySelectionExceptionAnalyzerTests.InvalidDialect testProperties) {
		}

	}

	@ConfigurationProperties("spring.jpa")
	private static class ValidDialect {

		String databasePlatform = "org.hibernate.dialect.MySQL5InnoDBDialect";

		public String getDatabasePlatform() {
			return this.databasePlatform;
		}

		public void setDatabasePlatform(String databasePlatform) {
			this.databasePlatform = databasePlatform;
		}

	}

	@EnableConfigurationProperties(StrategySelectionExceptionAnalyzerTests.ValidDialect.class)
	static class ValidDialectConfiguration {

		ValidDialectConfiguration(
				StrategySelectionExceptionAnalyzerTests.ValidDialect testProperties) {
		}

	}

}
