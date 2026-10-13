/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.source;

import org.jspecify.annotations.Nullable;

/**
 * Interface used to indicate that a {@link ConfigurationPropertySource} supports
 * {@link ConfigurationPropertyCaching}.
 *
 * @author Phillip Webb
 */
interface CachingConfigurationPropertySource {

	/**
	 * Return {@link ConfigurationPropertyCaching} for this source.
	 * @return source caching
	 */
	ConfigurationPropertyCaching getCaching();

	/**
	 * Find {@link ConfigurationPropertyCaching} for the given source.
	 * @param source the configuration property source
	 * @return a {@link ConfigurationPropertyCaching} instance or {@code null} if the
	 * source does not support caching.
	 */
	static @Nullable ConfigurationPropertyCaching find(@Nullable ConfigurationPropertySource source) {
		if (source instanceof CachingConfigurationPropertySource cachingSource) {
			return cachingSource.getCaching();
		}
		return null;
	}

}
