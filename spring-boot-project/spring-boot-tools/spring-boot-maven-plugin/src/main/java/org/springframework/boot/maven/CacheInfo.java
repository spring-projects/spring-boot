/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.maven;

import org.springframework.boot.buildpack.platform.build.Cache;
import org.springframework.util.Assert;

/**
 * Encapsulates configuration of an image building cache.
 *
 * @author Scott Frederick
 * @since 2.6.0
 */
public class CacheInfo {

	private Cache cache;

	/**
	 * Constructs a new CacheInfo object.
	 */
	public CacheInfo() {
	}

	/**
	 * Constructs a new CacheInfo object with the specified cache.
	 * @param cache the cache to be associated with the CacheInfo object
	 */
	private CacheInfo(Cache cache) {
		this.cache = cache;
	}

	/**
	 * Sets the volume cache information.
	 * @param info the volume cache information to be set
	 * @throws IllegalStateException if the cache has already been configured
	 */
	public void setVolume(VolumeCacheInfo info) {
		Assert.state(this.cache == null, "Each image building cache can be configured only once");
		this.cache = Cache.volume(info.getName());
	}

	/**
	 * Sets the bind cache information.
	 * @param info the bind cache information to be set
	 * @throws IllegalStateException if the cache has already been configured
	 */
	public void setBind(BindCacheInfo info) {
		Assert.state(this.cache == null, "Each image building cache can be configured only once");
		this.cache = Cache.bind(info.getSource());
	}

	/**
	 * Returns the cache object.
	 * @return the cache object
	 */
	Cache asCache() {
		return this.cache;
	}

	/**
	 * Creates a new CacheInfo object from the provided VolumeCacheInfo object.
	 * @param cacheInfo the VolumeCacheInfo object to create the CacheInfo object from
	 * @return a new CacheInfo object
	 */
	static CacheInfo fromVolume(VolumeCacheInfo cacheInfo) {
		return new CacheInfo(Cache.volume(cacheInfo.getName()));
	}

	/**
	 * Creates a new CacheInfo object from the provided BindCacheInfo object.
	 * @param cacheInfo the BindCacheInfo object to create the CacheInfo object from
	 * @return a new CacheInfo object with the source of the BindCacheInfo object bound to
	 * the Cache
	 */
	static CacheInfo fromBind(BindCacheInfo cacheInfo) {
		return new CacheInfo(Cache.bind(cacheInfo.getSource()));
	}

	/**
	 * Encapsulates configuration of an image building cache stored in a volume.
	 */
	public static class VolumeCacheInfo {

		private String name;

		/**
		 * Constructs a new VolumeCacheInfo object.
		 */
		public VolumeCacheInfo() {
		}

		/**
		 * Constructs a new VolumeCacheInfo object with the specified name.
		 * @param name the name of the volume cache info
		 */
		VolumeCacheInfo(String name) {
			this.name = name;
		}

		/**
		 * Returns the name of the VolumeCacheInfo object.
		 * @return the name of the VolumeCacheInfo object
		 */
		public String getName() {
			return this.name;
		}

		/**
		 * Sets the name of the VolumeCacheInfo.
		 * @param name the name to be set for the VolumeCacheInfo
		 */
		void setName(String name) {
			this.name = name;
		}

	}

	/**
	 * Encapsulates configuration of an image building cache stored in a bind mount.
	 */
	public static class BindCacheInfo {

		private String source;

		/**
		 * Constructs a new BindCacheInfo object.
		 */
		public BindCacheInfo() {
		}

		/**
		 * Binds the cache information with the specified name.
		 * @param name the name of the cache information to be bound
		 */
		BindCacheInfo(String name) {
			this.source = name;
		}

		/**
		 * Returns the source of the BindCacheInfo.
		 * @return the source of the BindCacheInfo
		 */
		public String getSource() {
			return this.source;
		}

		/**
		 * Sets the source of the BindCacheInfo.
		 * @param source the source to set
		 */
		void setSource(String source) {
			this.source = source;
		}

	}

}
