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

package org.springframework.boot.test.http.server;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A base URL that can be used to connect to the running server.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public interface BaseUrl {

	/**
	 * Default base URL suitable for mock environments.
	 */
	BaseUrl DEFAULT = BaseUrl.of("http://localhost");

	/**
	 * Return if the URL will ultimately resolve to an HTTPS address.
	 * @return if the URL is HTTPS
	 */
	boolean isHttps();

	/**
	 * Resolve the URL to a string. This method is called as late as possible to ensure
	 * that an local port information is available.
	 * @param path the path to append
	 * @return the resolved base URL
	 */
	default String resolve(@Nullable String path) {
		String resolved = resolve();
		if (StringUtils.hasLength(path)) {
			if (resolved.endsWith("/") && path.startsWith("/")) {
				path = path.substring(1);
			}
			resolved += (resolved.endsWith("/") || path.startsWith("/")) ? "" : "/";
			resolved += path;
		}
		return resolved;
	}

	/**
	 * Resolve the URL to a string. This method is called as late as possible to ensure
	 * that an local port information is available.
	 * @return the resolved base URL
	 */
	String resolve();

	/**
	 * Factory method to create a new {@link BaseUrl}.
	 * @param url the URL to use
	 * @return a new {@link BaseUrl} instance
	 */
	static BaseUrl of(String url) {
		Assert.notNull(url, "'url' must not be null");
		return of(StringUtils.startsWithIgnoreCase(url, "https"), () -> url);
	}

	/**
	 * Factory method to create a new {@link BaseUrl}.
	 * @param https whether the base URL is https
	 * @param resolver the resolver used to supply the actual URL
	 * @return a new {@link BaseUrl} instance
	 */
	static BaseUrl of(boolean https, Supplier<String> resolver) {
		Assert.notNull(resolver, "'resolver' must not be null");
		return new BaseUrl() {

			@Override
			public boolean isHttps() {
				return https;
			}

			@Override
			public String resolve() {
				return resolver.get();
			}

		};
	}

}
