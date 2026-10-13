/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.util.Assert;

/**
 * Adapts a {@link HealthIndicator} to a {@link ReactiveHealthIndicator} so that it can be
 * safely invoked in a reactive environment.
 *
 * @author Stephane Nicoll
 * @see ReactiveHealthContributor#adapt(HealthContributor)
 */
class HealthIndicatorAdapter implements ReactiveHealthIndicator {

	private final HealthIndicator delegate;

	HealthIndicatorAdapter(HealthIndicator delegate) {
		Assert.notNull(delegate, "'delegate' must not be null");
		this.delegate = delegate;
	}

	@Override
	public Mono<Health> health() {
		return Mono.fromCallable(this.delegate::health).subscribeOn(Schedulers.boundedElastic());
	}

}
