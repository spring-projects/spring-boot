/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import org.springframework.data.couchbase.cache.CouchbaseCacheManager;
import org.springframework.data.couchbase.cache.CouchbaseCacheManager.CouchbaseCacheManagerBuilder;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link CouchbaseCacheManagerBuilder} to fine-tune its auto-configuration before it is
 * used to build the auto-configured {@link CouchbaseCacheManager}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface CouchbaseCacheManagerBuilderCustomizer {

	/**
	 * Customize the {@link CouchbaseCacheManagerBuilder}.
	 * @param builder the builder to customize
	 */
	void customize(CouchbaseCacheManagerBuilder builder);

}
