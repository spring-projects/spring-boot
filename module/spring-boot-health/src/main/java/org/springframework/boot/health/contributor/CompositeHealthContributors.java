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
 * {@link HealthContributors} composed of other {@link HealthContributors}.
 *
 * @author Phillip Webb
 * @see HealthContributors#of(HealthContributors...)
 */
class CompositeHealthContributors implements HealthContributors {

	private final List<HealthContributors> contributors;

	CompositeHealthContributors(HealthContributors... contributors) {
		this.contributors = List.of(contributors);
	}

	@Override
	public @Nullable HealthContributor getContributor(String name) {
		return this.contributors.stream()
			.map((contributors) -> contributors.getContributor(name))
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	@Override
	public Stream<Entry> stream() {
		Set<String> seen = new HashSet<>();
		return this.contributors.stream()
			.flatMap(HealthContributors::stream)
			.filter((element) -> seen.add(element.name()));
	}

}
