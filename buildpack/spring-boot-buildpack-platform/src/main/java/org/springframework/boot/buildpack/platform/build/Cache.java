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
