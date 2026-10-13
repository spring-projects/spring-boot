/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHealthIndicator}.
 *
 * @author Phillip Webb
 */
class ReactiveHealthIndicatorTests {

	private final ReactiveHealthIndicator indicator = () -> Mono.just(Health.up().withDetail("spring", "boot").build());

	@Test
	void asHealthContributor() {
		HealthIndicator adapted = this.indicator.asHealthContributor();
		Health health = adapted.health(true);
		assertThat(health).isNotNull();
		assertThat(health.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getHealthWhenIncludeDetailsIsTrueReturnsHealthWithDetails() {
		Health health = this.indicator.health(true).block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("spring", "boot");
	}

	@Test
	void getHealthWhenIncludeDetailsIsFalseReturnsHealthWithoutDetails() {
		Health health = this.indicator.health(false).block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).isEmpty();
	}

}
