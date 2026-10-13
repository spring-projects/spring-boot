/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.maven;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.build.Cache;
import org.springframework.util.Assert;

/**
 * Encapsulates configuration of an image building cache.
 *
 * @author Scott Frederick
 * @since 2.6.0
 */
public class CacheInfo {

	private @Nullable Cache cache;

	public CacheInfo() {
	}

	private CacheInfo(Cache cache) {
		this.cache = cache;
	}

	public void setVolume(VolumeCacheInfo info) {
		Assert.state(this.cache == null, "Each image building cache can be configured only once");
		String name = info.getName();
		Assert.state(name != null, "'name' must not be null");
		this.cache = Cache.volume(name);
	}

	public void setBind(BindCacheInfo info) {
		Assert.state(this.cache == null, "Each image building cache can be configured only once");
		String source = info.getSource();
		Assert.state(source != null, "'source' must not be null");
		this.cache = Cache.bind(source);
	}

	@Nullable Cache asCache() {
		return this.cache;
	}

	static CacheInfo fromVolume(VolumeCacheInfo cacheInfo) {
		String name = cacheInfo.getName();
		Assert.state(name != null, "'name' must not be null");
		return new CacheInfo(Cache.volume(name));
	}

	static CacheInfo fromBind(BindCacheInfo cacheInfo) {
		String source = cacheInfo.getSource();
		Assert.state(source != null, "'source' must not be null");
		return new CacheInfo(Cache.bind(source));
	}

	/**
	 * Encapsulates configuration of an image building cache stored in a volume.
	 */
	public static class VolumeCacheInfo {

		private @Nullable String name;

		public VolumeCacheInfo() {
		}

		VolumeCacheInfo(String name) {
			this.name = name;
		}

		public @Nullable String getName() {
			return this.name;
		}

		void setName(@Nullable String name) {
			this.name = name;
		}

	}

	/**
	 * Encapsulates configuration of an image building cache stored in a bind mount.
	 */
	public static class BindCacheInfo {

		private @Nullable String source;

		public BindCacheInfo() {
		}

		BindCacheInfo(String name) {
			this.source = name;
		}

		public @Nullable String getSource() {
			return this.source;
		}

		void setSource(@Nullable String source) {
			this.source = source;
		}

	}

}
