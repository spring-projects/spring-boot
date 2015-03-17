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

/**
 * Configuration properties for the cache abstraction.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.cache")
public class CacheProperties {

	/**
	 * Cache mode (can be "auto", "generic", "hazelcast", "jcache", "redis",
	 * "guava", "simple" or "none"). Auto-detected according to the
	 * environment by default.
	 */
	private String mode = "auto";

	/**
	 * The location of the configuration file to use to initialize the cache
	 * library.
	 */
	private Resource config;

	/**
	 * Comma-separated list of cache names to create if supported by the
	 * underlying cache manager. Usually, this disables the ability to
	 * create additional caches on-the-fly.
	 */
	private final List<String> cacheNames = new ArrayList<String>();

	private final JCache jcache = new JCache();

	private final Guava guava = new Guava();

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public Resource getConfig() {
		return config;
	}

	public void setConfig(Resource config) {
		this.config = config;
	}

	public List<String> getCacheNames() {
		return cacheNames;
	}

	public JCache getJcache() {
		return jcache;
	}

	public Guava getGuava() {
		return guava;
	}

	/**
	 * Resolve the config location if set.
	 * @return the location or {@code null} if it is not set
	 * @throws IllegalArgumentException if the config attribute is set to a unknown location
	 */
	public Resource resolveConfigLocation() {
		if (this.config != null) {
			if (this.config.exists()) {
				return this.config;
			}
			else {
				throw new IllegalArgumentException("Cache configuration field defined by " +
						"'spring.cache.config' does not exist " + this.config);
			}
		}
		return null;
	}

	/**
	 * JCache (JSR-107) specific cache properties.
	 */
	public static class JCache {

		/**
		 * Fully qualified name of the CachingProvider implementation to use to
		 * retrieve the JSR-107 compliant cache manager. Only needed if more than
		 * one JSR-107 implementation is available on the classpath.
		 */
		private String provider;

		public String getProvider() {
			return provider;
		}

		public void setProvider(String provider) {
			this.provider = provider;
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
			return spec;
		}

		public void setSpec(String spec) {
			this.spec = spec;
		}
	}

}
