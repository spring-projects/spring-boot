/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthIndicator}.
 *
 * @author Phillip Webb
 */
class HealthIndicatorTests {

	private final HealthIndicator indicator = () -> Health.up().withDetail("spring", "boot").build();

	@Test
	void getHealthWhenIncludeDetailsIsTrueReturnsHealthWithDetails() {
		Health health = this.indicator.health(true);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getHealthWhenIncludeDetailsIsFalseReturnsHealthWithoutDetails() {
		Health health = this.indicator.health(false);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).isEmpty();
	}

}
