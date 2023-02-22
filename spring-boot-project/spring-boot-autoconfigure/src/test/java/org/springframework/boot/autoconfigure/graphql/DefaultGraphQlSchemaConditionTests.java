/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.graphql;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnGraphQlSchema}.
 *
 * @author Brian Clozel
 */
class DefaultGraphQlSchemaConditionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void matchesWhenSchemaFilesAreDetected() {
		this.contextRunner.withUserConfiguration(TestingConfiguration.class).run((context) -> {
			didMatch(context);
			assertThat(conditionReportMessage(context)).contains("@ConditionalOnGraphQlSchema found schemas")
				.contains("@ConditionalOnGraphQlSchema did not find GraphQlSourceBuilderCustomizer");
		});
	}

	@Test
	void matchesWhenCustomizerIsDetected() {
		this.contextRunner.withUserConfiguration(CustomCustomizerConfiguration.class, TestingConfiguration.class)
			.withPropertyValues("spring.graphql.schema.locations=classpath:graphql/missing")
			.run((context) -> {
				didMatch(context);
				assertThat(conditionReportMessage(context)).contains(
						"@ConditionalOnGraphQlSchema did not find schema files in locations 'classpath:graphql/missing/'")
					.contains("@ConditionalOnGraphQlSchema found customizer myBuilderCuystomizer");
			});
	}

	@Test
	void doesNotMatchWhenBothAreMissing() {
		this.contextRunner.withUserConfiguration(TestingConfiguration.class)
			.withPropertyValues("spring.graphql.schema.locations=classpath:graphql/missing")
			.run((context) -> {
				assertThat(context).doesNotHaveBean("success");
				assertThat(conditionReportMessage(context)).contains(
						"@ConditionalOnGraphQlSchema did not find schema files in locations 'classpath:graphql/missing/'")
					.contains("@ConditionalOnGraphQlSchema did not find GraphQlSourceBuilderCustomizer");
			});
	}

	private void didMatch(AssertableApplicationContext context) {
		assertThat(context).hasBean("success");
		assertThat(context.getBean("success")).isEqualTo("success");
	}

	private String conditionReportMessage(AssertableApplicationContext context) {
		Collection<ConditionEvaluationReport.ConditionAndOutcomes> conditionAndOutcomes = ConditionEvaluationReport
			.get(context.getSourceApplicationContext().getBeanFactory())
			.getConditionAndOutcomesBySource()
			.values();
		return conditionAndOutcomes.iterator().next().iterator().next().getOutcome().getMessage();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnGraphQlSchema
	static class TestingConfiguration {

		@Bean
		String success() {
			return "success";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomCustomizerConfiguration {

		@Bean
		GraphQlSourceBuilderCustomizer myBuilderCuystomizer() {
			return (builder) -> {

			};
		}

	}

}
