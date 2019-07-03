/*
 * Copyright 2012-2018 the original author or authors.
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

	private final Boolean samePort;

	/**
	 * Creates a new {@code EndpointMapping} using the given {@code path}.
	 * @param path the path
	 * @param samePort states true or false for same port as server
	 */
	public EndpointMapping(String path, Boolean samePort) {
		this.path = normalizePath(path);
		this.samePort = samePort;
	}

	public EndpointMapping(String path) {
		this(path, true);
	}

	/**
	 * Returns the path to which endpoints should be mapped.
	 * @return the path
	 */
	public String getPath() {
		return this.path;
	}

	public boolean isSamePort() {
		return this.samePort;
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
