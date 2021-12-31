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

import java.util.List;

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import graphql.execution.ExecutionStepInfo;
import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.GraphQLObjectType;
import io.micrometer.core.instrument.Tag;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * Factory methods for Tags associated with a GraphQL request.
 *
 * @author Brian Clozel
 * @since 2.7.0
 */
public final class GraphQlTags {

	private static final Tag OUTCOME_SUCCESS = Tag.of("outcome", "SUCCESS");

	private static final Tag OUTCOME_ERROR = Tag.of("outcome", "ERROR");

	private static final Tag UNKNOWN_ERRORTYPE = Tag.of("errorType", "UNKNOWN");

	private GraphQlTags() {

	}

	public static Tag executionOutcome(ExecutionResult result, @Nullable Throwable exception) {
		if (exception == null && result.getErrors().isEmpty()) {
			return OUTCOME_SUCCESS;
		}
		else {
			return OUTCOME_ERROR;
		}
	}

	public static Tag errorType(GraphQLError error) {
		ErrorClassification errorType = error.getErrorType();
		if (errorType instanceof ErrorType) {
			return Tag.of("errorType", ((ErrorType) errorType).name());
		}
		return UNKNOWN_ERRORTYPE;
	}

	public static Tag errorPath(GraphQLError error) {
		StringBuilder builder = new StringBuilder();
		List<Object> pathSegments = error.getPath();
		if (!CollectionUtils.isEmpty(pathSegments)) {
			builder.append('$');
			for (Object segment : pathSegments) {
				try {
					Integer.parseUnsignedInt(segment.toString());
					builder.append("[*]");
				}
				catch (NumberFormatException exc) {
					builder.append('.');
					builder.append(segment);
				}
			}
		}
		return Tag.of("errorPath", builder.toString());
	}

	public static Tag dataFetchingOutcome(@Nullable Throwable exception) {
		return (exception != null) ? OUTCOME_ERROR : OUTCOME_SUCCESS;
	}

	public static Tag dataFetchingPath(InstrumentationFieldFetchParameters parameters) {
		ExecutionStepInfo executionStepInfo = parameters.getExecutionStepInfo();
		StringBuilder dataFetchingPath = new StringBuilder();
		if (executionStepInfo.hasParent() && executionStepInfo.getParent().getType() instanceof GraphQLObjectType) {
			dataFetchingPath.append(((GraphQLObjectType) executionStepInfo.getParent().getType()).getName());
			dataFetchingPath.append('.');
		}
		dataFetchingPath.append(executionStepInfo.getPath().getSegmentName());
		return Tag.of("path", dataFetchingPath.toString());
	}

}
