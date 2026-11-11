/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cache.autoconfigure;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.cache.CacheType;
import org.springframework.util.Assert;

/**
 * Mappings between {@link CacheType} and {@code @Configuration}.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Sebastien Deleuze
 */
final class CacheConfigurations {

	private static final Map<CacheType, String> MAPPINGS;

	static {
		Map<CacheType, String> mappings = new EnumMap<>(CacheType.class);
		mappings.put(CacheType.GENERIC, GenericCacheConfiguration.class.getName());
		mappings.put(CacheType.HAZELCAST, HazelcastCacheConfiguration.class.getName());
		mappings.put(CacheType.INFINISPAN, InfinispanCacheConfiguration.class.getName());
		mappings.put(CacheType.JCACHE, JCacheCacheConfiguration.class.getName());
		mappings.put(CacheType.COUCHBASE, CouchbaseCacheConfiguration.class.getName());
		mappings.put(CacheType.REDIS, RedisCacheConfiguration.class.getName());
		mappings.put(CacheType.CAFFEINE, CaffeineCacheConfiguration.class.getName());
		mappings.put(CacheType.CACHE2K, Cache2kCacheConfiguration.class.getName());
		mappings.put(CacheType.SIMPLE, SimpleCacheConfiguration.class.getName());
		mappings.put(CacheType.NONE, NoOpCacheConfiguration.class.getName());
		MAPPINGS = Collections.unmodifiableMap(mappings);
	}

	private CacheConfigurations() {
	}

	static String getConfigurationClass(CacheType cacheType) {
		String configurationClassName = MAPPINGS.get(cacheType);
		Assert.state(configurationClassName != null, () -> "Unknown cache type " + cacheType);
		return configurationClassName;
	}

	static CacheType getType(String configurationClassName) {
		for (Map.Entry<CacheType, String> entry : MAPPINGS.entrySet()) {
			if (entry.getValue().equals(configurationClassName)) {
				return entry.getKey();
			}
		}
		throw new IllegalStateException("Unknown configuration class " + configurationClassName);
	}

}
