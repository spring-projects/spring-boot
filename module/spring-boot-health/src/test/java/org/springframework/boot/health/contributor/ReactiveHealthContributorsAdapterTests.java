/*
 * Copyright 2012-present the original author or authors.
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
