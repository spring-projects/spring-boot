/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.registry;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.health.contributor.HealthContributor;
import org.springframework.boot.health.contributor.HealthContributors;

/**
 * A mutable registry of {@link HealthContributor health contributors}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface HealthContributorRegistry extends HealthContributors {

	/**
	 * Register a contributor with the given {@code name}.
	 * @param name the name of the contributor
	 * @param contributor the contributor to register
	 * @throws IllegalStateException if the contributor cannot be registered with the
	 * given {@code name}.
	 */
	void registerContributor(String name, HealthContributor contributor);

	/**
	 * Unregister a previously registered contributor.
	 * @param name the name of the contributor to unregister
	 * @return the unregistered indicator, or {@code null} if no indicator was found in
	 * the registry for the given {@code name}.
	 */
	@Nullable HealthContributor unregisterContributor(String name);

}
