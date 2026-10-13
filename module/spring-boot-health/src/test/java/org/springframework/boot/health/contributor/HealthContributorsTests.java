/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.health.contributor.HealthContributors.Entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthContributors}.
 *
 * @author Phillip Webb
 */
class HealthContributorsTests {

	@Test
	void iteratorAdaptsStream() {
		Entry e1 = new Entry("e1", mock(HealthIndicator.class));
		Entry e2 = new Entry("e2", mock(HealthIndicator.class));
		HealthContributors contributors = new HealthContributors() {

			@Override
			public Stream<Entry> stream() {
				return Stream.of(e1, e2);
			}

			@Override
			public @Nullable HealthContributor getContributor(String name) {
				return null;
			}

		};
		assertThat(contributors).containsExactly(e1, e2);
	}

	@Test
	void createEntryWhenNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Entry("", mock(HealthIndicator.class)))
			.withMessage("'name' must not be empty");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createEntryWhenContributorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Entry("test", null))
			.withMessage("'contributor' must not be null");
	}

	@Test
	void ofCreatesComposite() {
		HealthContributors c = mock(HealthContributors.class);
		assertThat(HealthContributors.of(c)).isInstanceOf(CompositeHealthContributors.class);
	}

}
