/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveHealthContributor}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class ReactiveHealthContributorTests {

	@Test
	void adaptWhenHealthIndicatorReturnsHealthIndicatorReactiveAdapter() {
		HealthIndicator indicator = () -> Health.outOfService().build();
		ReactiveHealthContributor adapted = ReactiveHealthContributor.adapt(indicator);
		assertThat(adapted).isInstanceOf(HealthIndicatorAdapter.class);
		Health health = ((ReactiveHealthIndicator) adapted).health().block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
	}

	@Test
	void adaptWhenCompositeHealthContributorReturnsCompositeHealthContributorReactiveAdapter() {
		HealthIndicator indicator = () -> Health.outOfService().build();
		CompositeHealthContributor contributor = CompositeHealthContributor
			.fromMap(Collections.singletonMap("a", indicator));
		ReactiveHealthContributor adapted = ReactiveHealthContributor.adapt(contributor);
		assertThat(adapted).isInstanceOf(CompositeHealthContributorAdapter.class);
		ReactiveHealthContributor contained = ((CompositeReactiveHealthContributor) adapted).getContributor("a");
		assertThat(contained).isNotNull();
		Health health = ((ReactiveHealthIndicator) contained).health().block();
		assertThat(health).isNotNull();
		assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
	}

}
