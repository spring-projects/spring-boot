/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.contributor;

import org.jspecify.annotations.Nullable;

/**
 * Directly contributes {@link Health} information for specific component or subsystem.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public non-sealed interface HealthIndicator extends HealthContributor {

	/**
	 * Return an indication of health.
	 * @param includeDetails if details should be included or removed
	 * @return the health
	 */
	default @Nullable Health health(boolean includeDetails) {
		Health health = health();
		if (health == null) {
			return null;
		}
		return includeDetails ? health : health.withoutDetails();
	}

	/**
	 * Return an indication of health.
	 * @return the health
	 */
	@Nullable Health health();

}
