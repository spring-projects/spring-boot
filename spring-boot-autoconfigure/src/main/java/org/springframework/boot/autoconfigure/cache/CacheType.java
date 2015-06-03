/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.cache;

/**
 * Supported cache types (defined in order of precedence).
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @since 1.3.0
 */
public enum CacheType {

	/**
	 * Generic caching using 'Cache' beans from the context.
	 */
	GENERIC(GenericCacheConfiguration.class),

	/**
	 * EhCache backed caching.
	 */
	EHCACHE(EhCacheCacheConfiguration.class),

	/**
	 * Hazelcast backed caching
	 */
	HAZELCAST(HazelcastCacheConfiguration.class),

	/**
	 * Infinispan backed caching.
	 */
	INFINISPAN(InfinispanCacheConfiguration.class),

	/**
	 * JCache (JSR-107) backed caching.
	 */
	JCACHE(JCacheCacheConfiguration.class),

	/**
	 * Redis backed caching.
	 */
	REDIS(RedisCacheConfiguration.class),

	/**
	 * Guava backed caching.
	 */
	GUAVA(GuavaCacheConfiguration.class),

	/**
	 * Simple in-memory caching.
	 */
	SIMPLE(SimpleCacheConfiguration.class),

	/**
	 * No caching.
	 */
	NONE(NoOpCacheConfiguration.class);

	private final Class<?> configurationClass;

	CacheType(Class<?> configurationClass) {
		this.configurationClass = configurationClass;
	}

	Class<?> getConfigurationClass() {
		return this.configurationClass;
	}

	static CacheType forConfigurationClass(String configurationClass) {
		for (CacheType type : values()) {
			if (type.getConfigurationClass().getName().equals(configurationClass)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Unsupported class " + configurationClass);
	}

}
