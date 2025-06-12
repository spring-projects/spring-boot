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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import org.springframework.boot.health.contributor.HealthContributors.Entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link ReactiveHealthContributorsAdapter}.
 *
 * @author Phillip Webb
 */
class ReactiveHealthContributorsAdapterTests {

	@Test
	void getContributorWhenNoneReturnsNull() {
		ReactiveHealthIndicator a = mock(ReactiveHealthIndicator.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		CompositeReactiveHealthContributor delegate = CompositeReactiveHealthContributor.fromMap(Map.of("a", a));
		ReactiveHealthContributorsAdapter adapter = createAdapter(delegate);
		assertThat(adapter.getContributor("x")).isNull();
	}

	@Test
	void getContributorReturnsAdaptedContributor() {
		ReactiveHealthIndicator a = mock(ReactiveHealthIndicator.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		CompositeReactiveHealthContributor delegate = CompositeReactiveHealthContributor.fromMap(Map.of("a", a));
		ReactiveHealthContributorsAdapter adapter = createAdapter(delegate);
		assertThat(adapter.getContributor("a")).isInstanceOf(ReactiveHealthIndicatorAdapter.class);
		assertThat(adapter.getContributor("a")).extracting("delegate").isSameAs(a);
	}

	@Test
	void streamAdaptsEntries() {
		ReactiveHealthIndicator a = mock(ReactiveHealthIndicator.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		ReactiveHealthIndicator b = mock(ReactiveHealthIndicator.class,
				withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
		Map<String, ReactiveHealthIndicator> map = new LinkedHashMap<>();
		map.put("a", a);
		map.put("b", b);
		CompositeReactiveHealthContributor delegate = CompositeReactiveHealthContributor.fromMap(map);
		ReactiveHealthContributorsAdapter adapter = createAdapter(delegate);
		assertThat(adapter.stream().map(Entry::name)).containsExactly("a", "b");
		Object[] contributors = adapter.stream().map(Entry::contributor).toArray();
		assertThat(contributors[0]).isInstanceOf(ReactiveHealthIndicatorAdapter.class);
		assertThat(contributors[0]).extracting("delegate").isSameAs(a);
		assertThat(contributors[1]).isInstanceOf(ReactiveHealthIndicatorAdapter.class);
		assertThat(contributors[1]).extracting("delegate").isSameAs(b);
	}

	protected ReactiveHealthContributorsAdapter createAdapter(CompositeReactiveHealthContributor delegate) {
		return new CompositeReactiveHealthContributorAdapter(delegate);
	}

}
