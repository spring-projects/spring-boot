/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.graphql;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.metrics.graphql.DefaultGraphQlTagsProvider;
import org.springframework.boot.actuate.metrics.graphql.GraphQlMetricsInstrumentation;
import org.springframework.boot.actuate.metrics.graphql.GraphQlTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphQlMetricsAutoConfiguration}.
 *
 * @author Brian Clozel
 */
class GraphQlMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(GraphQlMetricsAutoConfiguration.class));

	@Test
	void backsOffWhenMeterRegistryIsMissing() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(GraphQlMetricsAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(DefaultGraphQlTagsProvider.class)
						.doesNotHaveBean(GraphQlMetricsInstrumentation.class));
	}

	@Test
	void definesTagsProviderAndInstrumentationWhenMeterRegistryIsPresent() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(DefaultGraphQlTagsProvider.class)
				.hasSingleBean(GraphQlMetricsInstrumentation.class));
	}

	@Test
	void tagsProviderBacksOffIfAlreadyPresent() {
		this.contextRunner.withUserConfiguration(TagsProviderConfiguration.class).run((context) -> assertThat(context)
				.doesNotHaveBean(DefaultGraphQlTagsProvider.class).hasSingleBean(TestGraphQlTagsProvider.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class TagsProviderConfiguration {

		@Bean
		TestGraphQlTagsProvider tagsProvider() {
			return new TestGraphQlTagsProvider();
		}

	}

	static class TestGraphQlTagsProvider implements GraphQlTagsProvider {

		@Override
		public Iterable<Tag> getExecutionTags(InstrumentationExecutionParameters parameters, ExecutionResult result,
				Throwable exception) {
			return null;
		}

		@Override
		public Iterable<Tag> getErrorTags(InstrumentationExecutionParameters parameters, GraphQLError error) {
			return null;
		}

		@Override
		public Iterable<Tag> getDataFetchingTags(DataFetcher<?> dataFetcher,
				InstrumentationFieldFetchParameters parameters, Throwable exception) {
			return null;
		}

	}

}
