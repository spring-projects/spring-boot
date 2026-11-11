/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.autoconfigure.contributor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;

/**
 * Member predicate that matches based on {@code include} and {@code exclude} sets.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class IncludeExcludeHealthContributorMembership implements HealthContributorMembership {

	private final Set<String> include;

	private final Set<String> exclude;

	IncludeExcludeHealthContributorMembership(@Nullable Set<String> include, @Nullable Set<String> exclude) {
		this.include = clean(include);
		this.exclude = clean(exclude);
	}

	@Override
	public boolean isMember(String name) {
		name = clean(name);
		return isIncluded(name) && !isExcluded(name);
	}

	private boolean isIncluded(String name) {
		return this.include.isEmpty() || this.include.contains("*") || isIncludedName(name);
	}

	private boolean isIncludedName(String name) {
		if (this.include.contains(name)) {
			return true;
		}
		if (name.contains("/")) {
			String parent = name.substring(0, name.lastIndexOf("/"));
			return isIncludedName(parent);
		}
		return false;
	}

	private boolean isExcluded(String name) {
		return this.exclude.contains("*") || isExcludedName(name);
	}

	private boolean isExcludedName(String name) {
		if (this.exclude.contains(name)) {
			return true;
		}
		if (name.contains("/")) {
			String parent = name.substring(0, name.lastIndexOf("/"));
			return isExcludedName(parent);
		}
		return false;
	}

	private Set<String> clean(@Nullable Set<String> names) {
		if (names == null) {
			return Collections.emptySet();
		}
		Set<String> cleaned = names.stream().map(this::clean).collect(Collectors.toCollection(LinkedHashSet::new));
		return Collections.unmodifiableSet(cleaned);
	}

	@Contract("!null -> !null")
	private @Nullable String clean(@Nullable String name) {
		return (name != null) ? name.trim() : null;
	}

}
