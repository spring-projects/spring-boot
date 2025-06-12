/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.health.contributor;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.boot.health.contributor.ReactiveHealthContributors.Entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link ReactiveHealthContributors}.
 *
 * @author Phillip Webb
 */
class ReactiveHealthContributorsTests {

	@Test
	void iteratorAdaptsStream() {
		Entry e1 = new Entry("e1", mock(ReactiveHealthIndicator.class));
		Entry e2 = new Entry("e2", mock(ReactiveHealthIndicator.class));
		ReactiveHealthContributors contributors = new ReactiveHealthContributors() {

			@Override
			public Stream<Entry> stream() {
				return Stream.of(e1, e2);
			}

			@Override
			public ReactiveHealthContributor getContributor(String name) {
				return null;
			}

		};
		assertThat(contributors).containsExactly(e1, e2);
	}

	@Test
	void asHealthContributorsReturnsAdapter() {
		ReactiveHealthContributors contributors = mock(ReactiveHealthContributors.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		assertThat(contributors.asHealthContributors()).isInstanceOf(ReactiveHealthContributorsAdapter.class);
	}

	@Test
	void ofCreateComposite() {
		ReactiveHealthContributors c = mock(ReactiveHealthContributors.class);
		assertThat(ReactiveHealthContributors.of(c)).isInstanceOf(CompositeReactiveHealthContributors.class);
	}

	@Test
	void adaptWhenNullReturnsNull() {
		assertThat(ReactiveHealthContributors.adapt(null)).isNull();
	}

	@Test
	void adaptReturnsAdapter() {
		HealthContributors c = mock(HealthContributors.class);
		assertThat(ReactiveHealthContributors.adapt(c)).isInstanceOf(HealthContributorsAdapter.class);
	}

	@Test
	void createEntryWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Entry("", mock(ReactiveHealthIndicator.class)))
			.withMessage("'name' must not be empty");
	}

	@Test
	void createEntryWhenContributorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Entry("test", null))
			.withMessage("'contributor' must not be null");
	}

}
