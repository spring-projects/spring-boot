/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;

/**
 * Adapts {@link ReactiveHealthContributors} to {@link HealthContributors}.
 *
 * @author Phillip Webb
 * @see ReactiveHealthContributors#asHealthContributors()
 */
class ReactiveHealthContributorsAdapter implements HealthContributors {

	private final ReactiveHealthContributors delegate;

	ReactiveHealthContributorsAdapter(ReactiveHealthContributors delegate) {
		this.delegate = delegate;
	}

	@Override
	public @Nullable HealthContributor getContributor(String name) {
		return adapt(this.delegate.getContributor(name));
	}

	@Override
	public Stream<Entry> stream() {
		return this.delegate.stream().map((entry) -> new Entry(entry.name(), adapt(entry.contributor())));
	}

	@Contract("!null -> !null")
	private @Nullable HealthContributor adapt(@Nullable ReactiveHealthContributor contributor) {
		return (contributor != null) ? contributor.asHealthContributor() : null;
	}

}
