/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.util.StringUtils;

/**
 * A value object for the base mapping for endpoints.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class EndpointMapping {

	private final String path;

	/**
	 * Creates a new {@code EndpointMapping} using the given {@code path}.
	 * @param path the path
	 */
	public EndpointMapping(String path) {
		this.path = normalizePath(path);
	}

	/**
	 * Returns the path to which endpoints should be mapped.
	 * @return the path
	 */
	public String getPath() {
		return this.path;
	}

	public String createSubPath(String path) {
		return this.path + normalizePath(path);
	}

	private static String normalizePath(String path) {
		if (!StringUtils.hasText(path)) {
			return path;
		}
		String normalizedPath = path;
		if (!normalizedPath.startsWith("/")) {
			normalizedPath = "/" + normalizedPath;
		}
		if (normalizedPath.endsWith("/")) {
			normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
		}
		return normalizedPath;
	}

}
