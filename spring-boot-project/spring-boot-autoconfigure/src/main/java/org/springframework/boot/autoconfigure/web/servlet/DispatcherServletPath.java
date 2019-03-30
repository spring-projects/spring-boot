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
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Interface that can be used by auto-configurations that need path details for the
 * {@link DispatcherServletAutoConfiguration#DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME
 * default} {@link DispatcherServlet}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @since 2.0.4
 */
@FunctionalInterface
public interface DispatcherServletPath {

	/**
	 * Returns the configured path of the dispatcher servlet.
	 * @return the configured path
	 */
	String getPath();

	/**
	 * Return a form of the given path that's relative to the dispatcher servlet path.
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
	 * {@link ServletRegistrationBean} to map the dispatcher servlet.
	 * @return the path as a servlet URL mapping
	 */
	default String getServletUrlMapping() {
		if (getPath().equals("") || getPath().equals("/")) {
			return "/";
		}
		if (getPath().contains("*")) {
			return getPath();
		}
		if (getPath().endsWith("/")) {
			return getPath() + "*";
		}
		return getPath() + "/*";
	}

}
