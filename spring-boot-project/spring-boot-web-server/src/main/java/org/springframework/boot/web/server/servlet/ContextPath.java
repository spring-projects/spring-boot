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

package org.springframework.boot.web.server.servlet;

import org.springframework.util.Assert;

/**
 * The context path of a servlet web server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public final class ContextPath {

	/**
	 * The default context path.
	 */
	public static final ContextPath DEFAULT = ContextPath.of("");

	private final String path;

	private ContextPath(String path) {
		this.path = path;
	}

	public static ContextPath of(String contextPath) {
		Assert.notNull(contextPath, "'contextPath' must not be null");
		if (!contextPath.isEmpty()) {
			if ("/".equals(contextPath)) {
				throw new IllegalArgumentException("Root context path must be specified using an empty string");
			}
			if (!contextPath.startsWith("/") || contextPath.endsWith("/")) {
				throw new IllegalArgumentException("Context path must start with '/' and not end with '/'");
			}
		}
		return new ContextPath(contextPath);
	}

	@Override
	public String toString() {
		return this.path;
	}

}
