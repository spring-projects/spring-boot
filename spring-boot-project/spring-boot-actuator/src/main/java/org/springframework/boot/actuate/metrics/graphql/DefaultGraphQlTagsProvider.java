/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.boot.actuate.metrics.graphql;

import java.util.Collections;
import java.util.List;

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * Default implementation for {@link GraphQlTagsProvider}.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
public class DefaultGraphQlTagsProvider implements GraphQlTagsProvider {

	private final List<GraphQlTagsContributor> contributors;

	public DefaultGraphQlTagsProvider(List<GraphQlTagsContributor> contributors) {
		this.contributors = contributors;
	}

	public DefaultGraphQlTagsProvider() {
		this(Collections.emptyList());
	}

	@Override
	public Iterable<Tag> getExecutionTags(InstrumentationExecutionParameters parameters, ExecutionResult result,
			Throwable exception) {
		Tags tags = Tags.of(GraphQlTags.executionOutcome(result, exception));
		for (GraphQlTagsContributor contributor : this.contributors) {
			tags = tags.and(contributor.getExecutionTags(parameters, result, exception));
		}
		return tags;
	}

	@Override
	public Iterable<Tag> getErrorTags(InstrumentationExecutionParameters parameters, GraphQLError error) {
		Tags tags = Tags.of(GraphQlTags.errorType(error), GraphQlTags.errorPath(error));
		for (GraphQlTagsContributor contributor : this.contributors) {
			tags = tags.and(contributor.getErrorTags(parameters, error));
		}
		return tags;
	}

	@Override
	public Iterable<Tag> getDataFetchingTags(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters,
			Throwable exception) {
		Tags tags = Tags.of(GraphQlTags.dataFetchingOutcome(exception), GraphQlTags.dataFetchingPath(parameters));
		for (GraphQlTagsContributor contributor : this.contributors) {
			tags = tags.and(contributor.getDataFetchingTags(dataFetcher, parameters, exception));
		}
		return tags;
	}

}
