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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Configuration properties for the cache abstraction.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Ryon Day
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.cache")
public class CacheProperties {

	/**
	 * Cache type. By default, auto-detected according to the environment.
	 */
	private CacheType type;

	/**
	 * Comma-separated list of cache names to create if supported by the underlying cache
	 * manager. Usually, this disables the ability to create additional caches on-the-fly.
	 */
	private List<String> cacheNames = new ArrayList<>();

	private final Caffeine caffeine = new Caffeine();

	private final Couchbase couchbase = new Couchbase();

	private final Infinispan infinispan = new Infinispan();

	private final JCache jcache = new JCache();

	private final Redis redis = new Redis();

	/**
     * Returns the type of cache.
     * 
     * @return the type of cache
     */
    public CacheType getType() {
		return this.type;
	}

	/**
     * Sets the type of cache.
     * 
     * @param mode the cache type to be set
     */
    public void setType(CacheType mode) {
		this.type = mode;
	}

	/**
     * Returns the list of cache names.
     *
     * @return the list of cache names
     */
    public List<String> getCacheNames() {
		return this.cacheNames;
	}

	/**
     * Sets the names of the caches.
     * 
     * @param cacheNames the list of cache names to be set
     */
    public void setCacheNames(List<String> cacheNames) {
		this.cacheNames = cacheNames;
	}

	/**
     * Returns the caffeine object associated with this CacheProperties instance.
     *
     * @return the caffeine object
     */
    public Caffeine getCaffeine() {
		return this.caffeine;
	}

	/**
     * Returns the Couchbase instance associated with this CacheProperties object.
     *
     * @return the Couchbase instance
     */
    public Couchbase getCouchbase() {
		return this.couchbase;
	}

	/**
     * Returns the Infinispan instance associated with this CacheProperties object.
     *
     * @return the Infinispan instance
     */
    public Infinispan getInfinispan() {
		return this.infinispan;
	}

	/**
     * Returns the JCache instance associated with this CacheProperties object.
     *
     * @return the JCache instance
     */
    public JCache getJcache() {
		return this.jcache;
	}

	/**
     * Returns the Redis instance associated with this CacheProperties object.
     *
     * @return the Redis instance
     */
    public Redis getRedis() {
		return this.redis;
	}

	/**
	 * Resolve the config location if set.
	 * @param config the config resource
	 * @return the location or {@code null} if it is not set
	 * @throws IllegalArgumentException if the config attribute is set to an unknown
	 * location
	 */
	public Resource resolveConfigLocation(Resource config) {
		if (config != null) {
			Assert.isTrue(config.exists(),
					() -> "Cache configuration does not exist '" + config.getDescription() + "'");
			return config;
		}
		return null;
	}

	/**
	 * Caffeine specific cache properties.
	 */
	public static class Caffeine {

		/**
		 * The spec to use to create caches. See CaffeineSpec for more details on the spec
		 * format.
		 */
		private String spec;

		/**
         * Returns the specification of the Caffeine.
         *
         * @return the specification of the Caffeine
         */
        public String getSpec() {
			return this.spec;
		}

		/**
         * Sets the specification of the Caffeine.
         * 
         * @param spec the specification of the Caffeine
         */
        public void setSpec(String spec) {
			this.spec = spec;
		}

	}

	/**
	 * Couchbase specific cache properties.
	 */
	public static class Couchbase {

		/**
		 * Entry expiration. By default the entries never expire. Note that this value is
		 * ultimately converted to seconds.
		 */
		private Duration expiration;

		/**
         * Returns the expiration duration of the Couchbase object.
         *
         * @return the expiration duration of the Couchbase object
         */
        public Duration getExpiration() {
			return this.expiration;
		}

		/**
         * Sets the expiration duration for the Couchbase object.
         * 
         * @param expiration the duration after which the object will expire
         */
        public void setExpiration(Duration expiration) {
			this.expiration = expiration;
		}

	}

	/**
	 * Infinispan specific cache properties.
	 */
	public static class Infinispan {

		/**
		 * The location of the configuration file to use to initialize Infinispan.
		 */
		private Resource config;

		/**
         * Retrieves the configuration resource.
         *
         * @return the configuration resource
         */
        public Resource getConfig() {
			return this.config;
		}

		/**
         * Sets the configuration resource for the Infinispan instance.
         * 
         * @param config the configuration resource to be set
         */
        public void setConfig(Resource config) {
			this.config = config;
		}

	}

	/**
	 * JCache (JSR-107) specific cache properties.
	 */
	public static class JCache {

		/**
		 * The location of the configuration file to use to initialize the cache manager.
		 * The configuration file is dependent of the underlying cache implementation.
		 */
		private Resource config;

		/**
		 * Fully qualified name of the CachingProvider implementation to use to retrieve
		 * the JSR-107 compliant cache manager. Needed only if more than one JSR-107
		 * implementation is available on the classpath.
		 */
		private String provider;

		/**
         * Returns the provider of the JCache.
         *
         * @return the provider of the JCache
         */
        public String getProvider() {
			return this.provider;
		}

		/**
         * Sets the provider for the JCache.
         * 
         * @param provider the provider to be set for the JCache
         */
        public void setProvider(String provider) {
			this.provider = provider;
		}

		/**
         * Returns the configuration resource.
         *
         * @return the configuration resource
         */
        public Resource getConfig() {
			return this.config;
		}

		/**
         * Sets the configuration resource for the JCache.
         * 
         * @param config the configuration resource to be set
         */
        public void setConfig(Resource config) {
			this.config = config;
		}

	}

	/**
	 * Redis-specific cache properties.
	 */
	public static class Redis {

		/**
		 * Entry expiration. By default the entries never expire.
		 */
		private Duration timeToLive;

		/**
		 * Allow caching null values.
		 */
		private boolean cacheNullValues = true;

		/**
		 * Key prefix.
		 */
		private String keyPrefix;

		/**
		 * Whether to use the key prefix when writing to Redis.
		 */
		private boolean useKeyPrefix = true;

		/**
		 * Whether to enable cache statistics.
		 */
		private boolean enableStatistics;

		/**
         * Returns the time to live (TTL) of the Redis object.
         *
         * @return the time to live (TTL) of the Redis object
         */
        public Duration getTimeToLive() {
			return this.timeToLive;
		}

		/**
         * Sets the time to live for the Redis object.
         * 
         * @param timeToLive the duration of time to live for the Redis object
         */
        public void setTimeToLive(Duration timeToLive) {
			this.timeToLive = timeToLive;
		}

		/**
         * Returns a boolean value indicating whether the Redis cache allows storing null values.
         *
         * @return {@code true} if the Redis cache allows storing null values, {@code false} otherwise.
         */
        public boolean isCacheNullValues() {
			return this.cacheNullValues;
		}

		/**
         * Sets whether to cache null values in Redis.
         * 
         * @param cacheNullValues true to cache null values, false otherwise
         */
        public void setCacheNullValues(boolean cacheNullValues) {
			this.cacheNullValues = cacheNullValues;
		}

		/**
         * Returns the key prefix used in Redis.
         *
         * @return the key prefix used in Redis
         */
        public String getKeyPrefix() {
			return this.keyPrefix;
		}

		/**
         * Sets the key prefix for Redis operations.
         * 
         * @param keyPrefix the key prefix to be set
         */
        public void setKeyPrefix(String keyPrefix) {
			this.keyPrefix = keyPrefix;
		}

		/**
         * Returns a boolean value indicating whether the use of key prefix is enabled.
         * 
         * @return true if the use of key prefix is enabled, false otherwise
         */
        public boolean isUseKeyPrefix() {
			return this.useKeyPrefix;
		}

		/**
         * Sets whether to use a key prefix for Redis operations.
         * 
         * @param useKeyPrefix true to use a key prefix, false otherwise
         */
        public void setUseKeyPrefix(boolean useKeyPrefix) {
			this.useKeyPrefix = useKeyPrefix;
		}

		/**
         * Returns a boolean value indicating whether statistics are enabled.
         * 
         * @return true if statistics are enabled, false otherwise
         */
        public boolean isEnableStatistics() {
			return this.enableStatistics;
		}

		/**
         * Sets the flag to enable or disable statistics tracking.
         * 
         * @param enableStatistics true to enable statistics tracking, false to disable it
         */
        public void setEnableStatistics(boolean enableStatistics) {
			this.enableStatistics = enableStatistics;
		}

	}

}
