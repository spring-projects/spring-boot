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

import java.util.List;

import graphql.execution.instrumentation.parameters.InstrumentationFieldFetchParameters;
import graphql.schema.DataFetcher;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultGraphQlTagsProvider}.
 *
 * @author Grzegorz Zgudka
 */
class DefaultGraphQlTagsProviderTest {

	private GraphQlTagsContributor firstContributor;

	private GraphQlTagsContributor secondContributor;

	private DataFetcher<?> dataFetcher;

	private InstrumentationFieldFetchParameters fieldFetchParameters;

	private DefaultGraphQlTagsProvider sut;

	@BeforeEach
	void setup() {
		this.firstContributor = mock(GraphQlTagsContributor.class);
		this.secondContributor = mock(GraphQlTagsContributor.class);
		this.dataFetcher = mock(DataFetcher.class);
		this.fieldFetchParameters = mock(InstrumentationFieldFetchParameters.class);
		this.sut = new DefaultGraphQlTagsProvider(List.of(this.firstContributor, this.secondContributor));
	}

	@Test
	void shouldCallContributorsWithDataFetcherResultAndConcatTheirTags() {
		String dataFetcherResult = "value";
		given(this.firstContributor.getDataFetchingTags(this.dataFetcher, dataFetcherResult, this.fieldFetchParameters,
				null)).willReturn(Tags.of("tag1", "val1"));
		given(this.secondContributor.getDataFetchingTags(this.dataFetcher, dataFetcherResult, this.fieldFetchParameters,
				null)).willReturn(Tags.of("tag2", "val2"));

		Iterable<Tag> tags = this.sut.getDataFetchingTags(this.dataFetcher, dataFetcherResult,
				this.fieldFetchParameters, null);

		assertThat(tags).containsExactly(Tag.of("tag1", "val1"), Tag.of("tag2", "val2"));
	}

	@Test
	void shouldCallContributorsWithDataFetcherResultAndDedupTheirTags() {
		String dataFetcherResult = "value";
		given(this.firstContributor.getDataFetchingTags(this.dataFetcher, dataFetcherResult, this.fieldFetchParameters,
				null)).willReturn(Tags.of("tag1", "val1"));
		given(this.secondContributor.getDataFetchingTags(this.dataFetcher, dataFetcherResult, this.fieldFetchParameters,
				null)).willReturn(Tags.of("tag1", "val2"));

		Iterable<Tag> tags = this.sut.getDataFetchingTags(this.dataFetcher, dataFetcherResult,
				this.fieldFetchParameters, null);

		assertThat(tags).containsExactly(Tag.of("tag1", "val2"));
	}

}
