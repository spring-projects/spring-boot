/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import org.jspecify.annotations.Nullable;

/**
 * Adapts a {@link ReactiveHealthIndicator} to a {@link HealthIndicator}.
 *
 * @author Phillip Webb
 * @see ReactiveHealthIndicator#asHealthContributor()
 */
class ReactiveHealthIndicatorAdapter implements HealthIndicator {

	private final ReactiveHealthIndicator delegate;

	ReactiveHealthIndicatorAdapter(ReactiveHealthIndicator indicator) {
		this.delegate = indicator;
	}

	@Override
	public @Nullable Health health(boolean includeDetails) {
		return this.delegate.health(includeDetails).block();
	}

	@Override
	public @Nullable Health health() {
		return this.delegate.health().block();
	}

}
