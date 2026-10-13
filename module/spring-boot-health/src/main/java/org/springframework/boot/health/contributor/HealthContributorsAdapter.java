/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * Adapts {@link HealthContributors} to {@link ReactiveHealthContributors}.
 *
 * @author Phillip Webb
 * @see ReactiveHealthContributors#adapt(HealthContributors)
 */
class HealthContributorsAdapter implements ReactiveHealthContributors {

	private final HealthContributors delegate;

	HealthContributorsAdapter(HealthContributors delegate) {
		this.delegate = delegate;
	}

	@Override
	public @Nullable ReactiveHealthContributor getContributor(String name) {
		return ReactiveHealthContributor.adapt(this.delegate.getContributor(name));
	}

	@Override
	public Stream<Entry> stream() {
		return this.delegate.stream()
			.map((entry) -> new Entry(entry.name(), ReactiveHealthContributor.adapt(entry.contributor())));
	}

}
