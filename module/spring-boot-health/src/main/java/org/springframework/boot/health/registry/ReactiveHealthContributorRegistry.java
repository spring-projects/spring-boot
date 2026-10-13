/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.registry;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.ReactiveHealthContributor;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;

/**
 * A mutable registry of {@link ReactiveHealthContributor reactive health contributors}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface ReactiveHealthContributorRegistry extends ReactiveHealthContributors {

	/**
	 * Register a contributor with the given {@code name}.
	 * @param name the name of the contributor
	 * @param contributor the contributor to register
	 * @throws IllegalStateException if the contributor cannot be registered with the
	 * given {@code name}.
	 */
	void registerContributor(String name, ReactiveHealthContributor contributor);

	/**
	 * Unregister a previously registered contributor.
	 * @param name the name of the contributor to unregister
	 * @return the unregistered indicator, or {@code null} if no indicator was found in
	 * the registry for the given {@code name}.
	 */
	@Nullable ReactiveHealthContributor unregisterContributor(String name);

}
