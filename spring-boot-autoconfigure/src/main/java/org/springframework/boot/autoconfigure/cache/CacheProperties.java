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
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.cache")
public class CacheProperties {

	/**
	 * Cache type, auto-detected according to the environment by default.
	 */
	private CacheType type;

	/**
	 * Comma-separated list of cache names to create if supported by the underlying cache
	 * manager. Usually, this disables the ability to create additional caches on-the-fly.
	 */
	private List<String> cacheNames = new ArrayList<String>();

	private final EhCache ehcache = new EhCache();

	private final Hazelcast hazelcast = new Hazelcast();

	private final Infinispan infinispan = new Infinispan();

	private final JCache jcache = new JCache();

	private final Guava guava = new Guava();

	public CacheType getType() {
		return this.type;
	}

	public void setType(CacheType mode) {
		this.type = mode;
	}

	public List<String> getCacheNames() {
		return this.cacheNames;
	}

	public void setCacheNames(List<String> cacheNames) {
		this.cacheNames = cacheNames;
	}

	public EhCache getEhcache() {
		return this.ehcache;
	}

	public Hazelcast getHazelcast() {
		return this.hazelcast;
	}

	public Infinispan getInfinispan() {
		return this.infinispan;
	}

	public JCache getJcache() {
		return this.jcache;
	}

	public Guava getGuava() {
		return this.guava;
	}

	/**
	 * Resolve the config location if set.
	 * @param config the config resource
	 * @return the location or {@code null} if it is not set
	 * @throws IllegalArgumentException if the config attribute is set to a unknown
	 * location
	 */
	public Resource resolveConfigLocation(Resource config) {
		if (config != null) {
			Assert.isTrue(config.exists(), "Cache configuration does not " + "exist '"
					+ config.getDescription() + "'");
			return config;
		}
		return null;
	}

	/**
	 * EhCache specific cache properties.
	 */
	public static class EhCache {

		/**
		 * The location of the configuration file to use to initialize EhCache.
		 */
		private Resource config;

		public Resource getConfig() {
			return this.config;
		}

		public void setConfig(Resource config) {
			this.config = config;
		}

	}

	/**
	 * Hazelcast specific cache properties.
	 */
	public static class Hazelcast {

		/**
		 * The location of the configuration file to use to initialize Hazelcast.
		 */
		private Resource config;

		public Resource getConfig() {
			return this.config;
		}

		public void setConfig(Resource config) {
			this.config = config;
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

		public Resource getConfig() {
			return this.config;
		}

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
		 * the JSR-107 compliant cache manager. Only needed if more than one JSR-107
		 * implementation is available on the classpath.
		 */
		private String provider;

		public String getProvider() {
			return this.provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
		}

		public Resource getConfig() {
			return this.config;
		}

		public void setConfig(Resource config) {
			this.config = config;
		}

	}

	/**
	 * Guava specific cache properties.
	 */
	public static class Guava {

		/**
		 * The spec to use to create caches. Check CacheBuilderSpec for more details on
		 * the spec format.
		 */
		private String spec;

		public String getSpec() {
			return this.spec;
		}

		public void setSpec(String spec) {
			this.spec = spec;
		}

	}

}
