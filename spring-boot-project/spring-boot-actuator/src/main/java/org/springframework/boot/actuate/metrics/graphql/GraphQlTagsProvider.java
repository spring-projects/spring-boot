/*
 * Copyright 2020-2021 the original author or authors.
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

import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.instrumentation.parameters.InstrumentationExecutionParameters;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import io.micrometer.core.instrument.Tag;

import org.springframework.lang.Nullable;

/**
 * Provides {@link Tag Tags} for Spring GraphQL-based request handling.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
public interface GraphQlTagsProvider {

	Iterable<Tag> getExecutionTags(InstrumentationExecutionParameters parameters, ExecutionResult result,
			@Nullable Throwable exception);

	Iterable<Tag> getErrorTags(InstrumentationExecutionParameters parameters, GraphQLError error);

	Iterable<Tag> getDataFetchingTags(DataFetcher<?> dataFetcher, InstrumentationFieldFetchParameters parameters,
			@Nullable Throwable exception);

}
