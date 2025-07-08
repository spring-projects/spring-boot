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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.ReactiveHealthContributors.Entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link CompositeReactiveHealthContributors}.
 *
 * @author Phillip Webb
 */
class CompositeReactiveHealthContributorsTests {

	@Test
	void getContributorWhenManyReturnsFirstContributor() {
		ReactiveHealthIndicator a1 = mock(ReactiveHealthIndicator.class);
		CompositeReactiveHealthContributor c1 = CompositeReactiveHealthContributor.fromMap(Map.of("a", a1));
		ReactiveHealthIndicator a2 = mock(ReactiveHealthIndicator.class);
		CompositeReactiveHealthContributor c2 = CompositeReactiveHealthContributor.fromMap(Map.of("a", a2));
		ReactiveHealthContributors composite = ReactiveHealthContributors.of(c1, c2);
		assertThat(composite.getContributor("a")).isSameAs(a1);
	}

	@Test
	void getContributorWhenNoneReturnsNull() {
		ReactiveHealthIndicator a = mock(ReactiveHealthIndicator.class);
		ReactiveHealthContributors composite = ReactiveHealthContributors
			.of(CompositeReactiveHealthContributor.fromMap(Map.of("a", a)));
		assertThat(composite.getContributor("x")).isNull();
	}

	@Test
	void streamStreamsWithoutDuplicates() {
		ReactiveHealthIndicator a1 = mock(ReactiveHealthIndicator.class);
		CompositeReactiveHealthContributor c1 = CompositeReactiveHealthContributor.fromMap(Map.of("a", a1));
		ReactiveHealthIndicator a2 = mock(ReactiveHealthIndicator.class);
		CompositeReactiveHealthContributor c2 = CompositeReactiveHealthContributor.fromMap(Map.of("a", a2));
		ReactiveHealthIndicator b = mock(ReactiveHealthIndicator.class);
		CompositeReactiveHealthContributor c3 = CompositeReactiveHealthContributor.fromMap(Map.of("b", b));
		ReactiveHealthContributors composite = ReactiveHealthContributors.of(c1, c2, c3);
		assertThat(composite.stream()).containsExactly(new Entry("a", a1), new Entry("b", b));
	}

}
