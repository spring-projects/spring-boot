/*
 * Copyright 2012-2019 the original author or authors.
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

	/**
	 * Creates a subpath by concatenating the current path with the provided path.
	 * @param path the path to be appended to the current path
	 * @return the concatenated subpath
	 */
	public String createSubPath(String path) {
		return this.path + normalizePath(path);
	}

	/**
	 * Normalizes the given path by adding a leading slash if it is missing and removing a
	 * trailing slash if present.
	 * @param path the path to be normalized
	 * @return the normalized path
	 */
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
