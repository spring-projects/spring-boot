/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import java.util.Iterator;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A collection of named {@link HealthContributor health contributors}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface HealthContributors extends Iterable<HealthContributors.Entry> {

	/**
	 * Return the contributor with the given name.
	 * @param name the name of the contributor
	 * @return a contributor instance or {@code null}
	 */
	@Nullable HealthContributor getContributor(String name);

	@Override
	default Iterator<Entry> iterator() {
		return stream().iterator();
	}

	/**
	 * Return a stream of the contributor entries.
	 * @return the stream of named contributors
	 */
	Stream<HealthContributors.Entry> stream();

	/**
	 * Factory method to create a new {@link HealthContributors} instance composed of the
	 * given contributors.
	 * @param contributors the source contributors in the order they should be combined
	 * @return a new {@link HealthContributors} instance
	 */
	static HealthContributors of(HealthContributors... contributors) {
		return new CompositeHealthContributors(contributors);
	}

	/**
	 * A health contributor entry.
	 *
	 * @param name the name of the contributor
	 * @param contributor the contributor
	 */
	record Entry(String name, HealthContributor contributor) {

		public Entry {
			Assert.hasText(name, "'name' must not be empty");
			Assert.notNull(contributor, "'contributor' must not be null");
		}

	}

}
