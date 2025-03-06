/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.cache;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

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
