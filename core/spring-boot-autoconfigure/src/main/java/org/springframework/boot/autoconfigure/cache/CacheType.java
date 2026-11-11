/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.cache;

/**
 * Supported cache types (defined in order of precedence).
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @since 4.0.0
 */
public enum CacheType {

	/**
	 * Generic caching using 'Cache' beans from the context.
	 */
	GENERIC,

	/**
	 * JCache (JSR-107) backed caching.
	 */
	JCACHE,

	/**
	 * Hazelcast backed caching.
	 */
	HAZELCAST,

	/**
	 * Couchbase backed caching.
	 */
	COUCHBASE,

	/**
	 * Infinispan backed caching.
	 */
	INFINISPAN,

	/**
	 * Redis backed caching.
	 */
	REDIS,

	/**
	 * Cache2k backed caching.
	 */
	CACHE2K,

	/**
	 * Caffeine backed caching.
	 */
	CAFFEINE,

	/**
	 * Simple in-memory caching.
	 */
	SIMPLE,

	/**
	 * No caching.
	 */
	NONE

}
