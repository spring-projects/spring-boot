/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.buildpack.platform.build.LocalCache.Bind;
import org.springframework.boot.buildpack.platform.build.LocalCache.Volume;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;
import org.springframework.util.Assert;

/**
 * Details of a cache for use by the CNB builder.
 *
 * @author Scott Frederick
 * @author Tim Ysewyn
 * @author Stephane Nicoll
 * @since 2.6.0
 */
public sealed interface Cache permits LocalCache, ImageCache {

	/**
	 * Return the details of the cache if it is a volume cache.
	 * @return the cache, or {@code null} if it is not a volume cache
	 */
	default LocalCache.@Nullable Volume getVolume() {
		return (this instanceof LocalCache.Volume volume) ? volume : null;
	}

	/**
	 * Return the details of the cache if it is a bind cache.
	 * @return the cache, or {@code null} if it is not a bind cache
	 */
	default LocalCache.@Nullable Bind getBind() {
		return (this instanceof LocalCache.Bind bind) ? bind : null;
	}

	/**
	 * Return the details of the cache if it is an image cache.
	 * @return the cache, or {@code null} if it is not an image cache
	 * @since 4.2.0
	 */
	default @Nullable ImageCache getImage() {
		return (this instanceof ImageCache image) ? image : null;
	}

	/**
	 * Create a new {@code Cache} that uses a volume with the provided name.
	 * @param name the cache volume name
	 * @return a new cache instance
	 */
	static LocalCache volume(String name) {
		Assert.notNull(name, "'name' must not be null");
		return new Volume(VolumeName.of(name));
	}

	/**
	 * Create a new {@code Cache} that uses a volume with the provided name.
	 * @param name the cache volume name
	 * @return a new cache instance
	 */
	static LocalCache volume(VolumeName name) {
		Assert.notNull(name, "'name' must not be null");
		return new Volume(name);
	}

	/**
	 * Create a new {@code Cache} that uses a bind mount with the provided source.
	 * @param source the cache bind mount source
	 * @return a new cache instance
	 */
	static LocalCache bind(String source) {
		Assert.notNull(source, "'source' must not be null");
		return new Bind(source);
	}

	/**
	 * Create a new {@code Cache} that uses an image with the provided name.
	 * @param name the cache image name
	 * @return a new cache instance
	 * @since 4.2.0
	 */
	static ImageCache image(String name) {
		Assert.notNull(name, "'name' must not be null");
		return new ImageCache(name);
	}

}
