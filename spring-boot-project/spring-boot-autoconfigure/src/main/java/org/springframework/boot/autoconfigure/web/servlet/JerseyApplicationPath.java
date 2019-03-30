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
package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.boot.web.servlet.ServletRegistrationBean;

/**
 * Interface that can be used by auto-configurations that need path details Jersey's
 * application path that serves as the base URI for the application.
 *
 * @author Madhura Bhave
 * @since 2.0.7
 */
@FunctionalInterface
public interface JerseyApplicationPath {

	/**
	 * Returns the configured path of the application.
	 * @return the configured path
	 */
	String getPath();

	/**
	 * Return a form of the given path that's relative to the Jersey application path.
	 * @param path the path to make relative
	 * @return the relative path
	 */
	default String getRelativePath(String path) {
		String prefix = getPrefix();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return prefix + path;
	}

	/**
	 * Return a cleaned up version of the path that can be used as a prefix for URLs. The
	 * resulting path will have path will not have a trailing slash.
	 * @return the prefix
	 * @see #getRelativePath(String)
	 */
	default String getPrefix() {
		String result = getPath();
		int index = result.indexOf('*');
		if (index != -1) {
			result = result.substring(0, index);
		}
		if (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	/**
	 * Return a URL mapping pattern that can be used with a
	 * {@link ServletRegistrationBean} to map Jersey's servlet.
	 * @return the path as a servlet URL mapping
	 */
	default String getUrlMapping() {
		String path = getPath();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (path.equals("/")) {
			return "/*";
		}
		if (path.contains("*")) {
			return path;
		}
		if (path.endsWith("/")) {
			return path + "*";
		}
		return path + "/*";
	}

}
