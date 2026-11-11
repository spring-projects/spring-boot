/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import java.util.Properties;

import javax.cache.CacheManager;
import javax.cache.spi.CachingProvider;

/**
 * Callback interface that can be implemented by beans wishing to customize the properties
 * used by the {@link CachingProvider} to create the {@link CacheManager}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see CachingProvider#getCacheManager(java.net.URI, ClassLoader, Properties)
 */
public interface JCachePropertiesCustomizer {

	/**
	 * Customize the properties.
	 * @param properties the current properties
	 */
	void customize(Properties properties);

}
