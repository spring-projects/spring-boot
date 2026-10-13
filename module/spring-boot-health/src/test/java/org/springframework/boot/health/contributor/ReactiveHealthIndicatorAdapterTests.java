/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHealthIndicatorAdapter}.
 *
 * @author Phillip Webb
 */
class ReactiveHealthIndicatorAdapterTests {

	@Test
	void getHealthReturnsDetails() {
		ReactiveHealthIndicator reactiveHealthIndicator = () -> Mono
			.just(Health.up().withDetail("test", "test").build());
		ReactiveHealthIndicatorAdapter adapter = new ReactiveHealthIndicatorAdapter(reactiveHealthIndicator);
		Health health = adapter.health();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsEntry("test", "test");
	}

	@Test
	void getHealthWithoutDetailsReturnsHealth() {
		ReactiveHealthIndicator reactiveHealthIndicator = () -> Mono
			.just(Health.up().withDetail("test", "test").build());
		ReactiveHealthIndicatorAdapter adapter = new ReactiveHealthIndicatorAdapter(reactiveHealthIndicator);
		Health health = adapter.health(false);
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).isEmpty();
	}

}
