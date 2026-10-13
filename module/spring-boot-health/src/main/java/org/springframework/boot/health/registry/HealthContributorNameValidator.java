/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.registry;

/**
 * Interface that can be used to validate names before they are added to a health
 * contributor registry.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface HealthContributorNameValidator {

	/**
	 * Validate the contributor name.
	 * @param name the name to validate
	 * @throws IllegalStateException on an invalid name
	 */
	void validate(String name) throws IllegalStateException;

}
