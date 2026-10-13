/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import reactor.core.publisher.Mono;

/**
 * Directly contributes {@link Health} information for specific reactive component or
 * subsystem.
 * <p>
 * This is non-blocking contract that is meant to be used in a reactive application. See
 * {@link HealthIndicator} for the traditional contract.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see HealthIndicator
 */
@FunctionalInterface
public non-sealed interface ReactiveHealthIndicator extends ReactiveHealthContributor {

	@Override
	default HealthIndicator asHealthContributor() {
		return new ReactiveHealthIndicatorAdapter(this);
	}

	/**
	 * Provide the indicator of health.
	 * @param includeDetails if details should be included or removed
	 * @return a {@link Mono} that provides the {@link Health}
	 */
	default Mono<Health> health(boolean includeDetails) {
		Mono<Health> health = health();
		return includeDetails ? health : health.map(Health::withoutDetails);
	}

	/**
	 * Provide the indicator of health.
	 * @return a {@link Mono} that provides the {@link Health}
	 */
	Mono<Health> health();

}
