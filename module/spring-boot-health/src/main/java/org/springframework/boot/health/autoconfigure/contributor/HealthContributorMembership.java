/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.contributor;

import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Determines if a health contributor is a member of a group or collection.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.1.0
 */
@FunctionalInterface
public interface HealthContributorMembership {

	/**
	 * Returns {@code true} if the given contributor is a member.
	 * @param name the contributor name
	 * @return {@code true} if the contributor is a member
	 */
	boolean isMember(String name);

	/**
	 * Return a {@link HealthContributorMembership} that always matches.
	 * @return a {@link HealthContributorMembership} instance that always matches
	 */
	static HealthContributorMembership always() {
		return (name) -> true;
	}

	/**
	 * Return a {@link HealthContributorMembership} instance that matches based on include
	 * and exclude patterns. Support {@code '*'} patterns and {@code '/'} separated names.
	 * @param include the include patterns or {@code null}
	 * @param exclude the exclude patterns of {@code null}
	 * @return a new {@link HealthContributorMembership} instance
	 */
	static HealthContributorMembership byIncludeExclude(@Nullable Set<String> include, @Nullable Set<String> exclude) {
		return new IncludeExcludeHealthContributorMembership(include, exclude);
	}

}
