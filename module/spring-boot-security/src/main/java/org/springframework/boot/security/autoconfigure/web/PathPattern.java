/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web;

import org.springframework.util.Assert;

/**
 * Utility class used to build a normalized pattern based on a user supplied path.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0.9.1
 */
public final class PathPattern {

	private final String normalizedPath;

	private PathPattern(String normalizedPath) {
		this.normalizedPath = normalizedPath;
	}

	/**
	 * Return a string containing the path pattern with a trailing slash.
	 * @return the pattern string
	 */
	public String withTrailingSlash() {
		return withoutTrailingSlash() + "/";
	}

	/**
	 * Return a string containing the path pattern without a trailing slash.
	 * @return the pattern string
	 */
	public String withoutTrailingSlash() {
		return this.normalizedPath;
	}

	@Override
	public String toString() {
		return this.normalizedPath;
	}

	public static PathPattern of(String path) {
		Assert.hasText(path, "'path' must not be empty");
		String normalizedPath = path;
		while (normalizedPath.endsWith("/")) {
			normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
		}
		return new PathPattern(normalizedPath);
	}

}
