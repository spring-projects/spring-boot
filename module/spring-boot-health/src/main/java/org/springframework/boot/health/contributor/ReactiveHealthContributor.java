/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import org.jspecify.annotations.Nullable;
import reactor.core.scheduler.Schedulers;

import org.springframework.lang.Contract;

/**
 * Contributes health information, either directly ({@link ReactiveHealthIndicator}) or
 * via other contributors ({@link CompositeReactiveHealthContributor}).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see ReactiveHealthIndicator
 * @see CompositeReactiveHealthContributor
 */
public sealed interface ReactiveHealthContributor permits ReactiveHealthIndicator, CompositeReactiveHealthContributor {

	/**
	 * Return this reactive contributor as a standard blocking {@link HealthContributor}.
	 * @return a blocking health contributor
	 */
	HealthContributor asHealthContributor();

	/**
	 * Adapts the given {@link HealthContributor} into a {@link ReactiveHealthContributor}
	 * by scheduling blocking calls to {@link Schedulers#boundedElastic()}.
	 * @param contributor the contributor to adapt or {@code null}
	 * @return the adapted contributor
	 */
	@Contract("!null -> !null")
	static @Nullable ReactiveHealthContributor adapt(@Nullable HealthContributor contributor) {
		if (contributor == null) {
			return null;
		}
		if (contributor instanceof HealthIndicator healthIndicator) {
			return new HealthIndicatorAdapter(healthIndicator);
		}
		if (contributor instanceof CompositeHealthContributor compositeHealthContributor) {
			return new CompositeHealthContributorAdapter(compositeHealthContributor);
		}
		throw new IllegalStateException("Unknown 'contributor' type");
	}

}
