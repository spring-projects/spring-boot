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

package org.springframework.boot.actuate.metrics.graphql;

import java.util.Arrays;

import graphql.ErrorType;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphQlTags}.
 *
 * @author Brian Clozel
 */
class GraphQlTagsTests {

	@Test
	void executionOutcomeShouldSucceed() {
		ExecutionResult result = ExecutionResultImpl.newExecutionResult().build();
		Tag outcomeTag = GraphQlTags.executionOutcome(result, null);
		assertThat(outcomeTag).isEqualTo(Tag.of("outcome", "SUCCESS"));
	}

	@Test
	void executionOutcomeShouldErrorWhenExceptionThrown() {
		ExecutionResult result = ExecutionResultImpl.newExecutionResult().build();
		Tag tag = GraphQlTags.executionOutcome(result, new IllegalArgumentException("test error"));
		assertThat(tag).isEqualTo(Tag.of("outcome", "ERROR"));
	}

	@Test
	void executionOutcomeShouldErrorWhenResponseErrors() {
		GraphQLError error = GraphqlErrorBuilder.newError().message("Invalid query").build();
		Tag tag = GraphQlTags.executionOutcome(ExecutionResultImpl.newExecutionResult().addError(error).build(), null);
		assertThat(tag).isEqualTo(Tag.of("outcome", "ERROR"));
	}

	@Test
	void errorTypeShouldBeDefinedIfPresent() {
		GraphQLError error = GraphqlErrorBuilder.newError().errorType(ErrorType.DataFetchingException)
				.message("test error").build();
		Tag errorTypeTag = GraphQlTags.errorType(error);
		assertThat(errorTypeTag).isEqualTo(Tag.of("error.type", "DataFetchingException"));
	}

	@Test
	void errorPathShouldUseJsonPathFormat() {
		GraphQLError error = GraphqlErrorBuilder.newError().path(Arrays.asList("project", "name")).message("test error")
				.build();
		Tag errorPathTag = GraphQlTags.errorPath(error);
		assertThat(errorPathTag).isEqualTo(Tag.of("error.path", "$.project.name"));
	}

	@Test
	void errorPathShouldUseJsonPathFormatForIndices() {
		GraphQLError error = GraphqlErrorBuilder.newError().path(Arrays.asList("issues", "42", "title"))
				.message("test error").build();
		Tag errorPathTag = GraphQlTags.errorPath(error);
		assertThat(errorPathTag).isEqualTo(Tag.of("error.path", "$.issues[*].title"));
	}

	@Test
	void dataFetchingOutcomeShouldBeSuccessfulIfNoException() {
		Tag fetchingOutcomeTag = GraphQlTags.dataFetchingOutcome(null);
		assertThat(fetchingOutcomeTag).isEqualTo(Tag.of("outcome", "SUCCESS"));
	}

	@Test
	void dataFetchingOutcomeShouldBeErrorIfException() {
		Tag fetchingOutcomeTag = GraphQlTags.dataFetchingOutcome(new IllegalStateException("error state"));
		assertThat(fetchingOutcomeTag).isEqualTo(Tag.of("outcome", "ERROR"));
	}

}
