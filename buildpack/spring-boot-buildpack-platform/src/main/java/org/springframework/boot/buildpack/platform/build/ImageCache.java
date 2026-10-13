/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Details of a cache stored in an image.
 *
 * @author Tim Ysewyn
 * @author Stephane Nicoll
 * @since 4.2.0
 */
public final class ImageCache implements Cache {

	private final String name;

	ImageCache(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ImageCache other = (ImageCache) obj;
		return Objects.equals(this.name, other.name);
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(this.name);
	}

	@Override
	public String toString() {
		return "image '" + this.name + "'";
	}

}
