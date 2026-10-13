/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
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
			public @Nullable ReactiveHealthContributor getContributor(String name) {
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
	@SuppressWarnings("NullAway") // Test null check
	void createEntryWhenContributorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Entry("test", null))
			.withMessage("'contributor' must not be null");
	}

}
