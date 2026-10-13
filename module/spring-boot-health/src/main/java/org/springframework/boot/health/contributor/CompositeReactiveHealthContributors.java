/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * {@link ReactiveHealthContributors} composed of other
 * {@link ReactiveHealthContributors}.
 *
 * @author Phillip Webb
 * @see ReactiveHealthContributors#of(ReactiveHealthContributors...)
 */
class CompositeReactiveHealthContributors implements ReactiveHealthContributors {

	private final List<ReactiveHealthContributors> contributors;

	CompositeReactiveHealthContributors(ReactiveHealthContributors... contributors) {
		this.contributors = List.of(contributors);
	}

	@Override
	public @Nullable ReactiveHealthContributor getContributor(String name) {
		return this.contributors.stream()
			.map((reactiveContributors) -> reactiveContributors.getContributor(name))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	@Override
	public Stream<Entry> stream() {
		Set<String> seen = new HashSet<>();
		return this.contributors.stream()
			.flatMap(ReactiveHealthContributors::stream)
			.filter((element) -> seen.add(element.name()));
	}

}
