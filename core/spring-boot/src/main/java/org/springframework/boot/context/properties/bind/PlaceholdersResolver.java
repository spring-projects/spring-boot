/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.PropertyResolver;

/**
 * Optional strategy that used by a {@link Binder} to resolve property placeholders.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 2.0.0
 * @see PropertySourcesPlaceholdersResolver
 */
@FunctionalInterface
public interface PlaceholdersResolver {

	/**
	 * No-op {@link PropertyResolver}.
	 */
	PlaceholdersResolver NONE = (value) -> value;

	/**
	 * Called to resolve any placeholders in the given value.
	 * @param value the source value
	 * @return a value with placeholders resolved
	 */
	@Nullable Object resolvePlaceholders(@Nullable Object value);

}
