/*
 * Copyright 2012-present the original author or authors.
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

	public CacheInfo() {
	}

	private CacheInfo(Cache cache) {
		this.cache = cache;
	}

	public void setVolume(VolumeCacheInfo info) {
		Assert.state(this.cache == null, "Each image building cache can be configured only once");
		this.cache = Cache.volume(info.getName());
	}

	public void setBind(BindCacheInfo info) {
		Assert.state(this.cache == null, "Each image building cache can be configured only once");
		this.cache = Cache.bind(info.getSource());
	}

	Cache asCache() {
		return this.cache;
	}

	static CacheInfo fromVolume(VolumeCacheInfo cacheInfo) {
		return new CacheInfo(Cache.volume(cacheInfo.getName()));
	}

	static CacheInfo fromBind(BindCacheInfo cacheInfo) {
		return new CacheInfo(Cache.bind(cacheInfo.getSource()));
	}

	/**
	 * Encapsulates configuration of an image building cache stored in a volume.
	 */
	public static class VolumeCacheInfo {

		private String name;

		public VolumeCacheInfo() {
		}

		VolumeCacheInfo(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		void setName(String name) {
			this.name = name;
		}

	}

	/**
	 * Encapsulates configuration of an image building cache stored in a bind mount.
	 */
	public static class BindCacheInfo {

		private String source;

		public BindCacheInfo() {
		}

		BindCacheInfo(String name) {
			this.source = name;
		}

		public String getSource() {
			return this.source;
		}

		void setSource(String source) {
			this.source = source;
		}

	}

}
