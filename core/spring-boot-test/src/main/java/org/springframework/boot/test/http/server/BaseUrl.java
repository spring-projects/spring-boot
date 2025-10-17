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

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * A base URL that can be used to connect to the running server.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
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
	 * Get a {@link UriBuilderFactory} that applies the base URL.
	 * @return a {@link UriBuilderFactory}
	 */
	UriBuilderFactory getUriBuilderFactory();

	/**
	 * Return a new instance that applies the given {@code path}.
	 * @param path a path to append
	 * @return a new instance with the path added
	 */
	BaseUrl withPath(String path);

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
		return new DefaultBaseUrl(https, resolver);
	}

	final class DefaultBaseUrl implements BaseUrl {

		private final boolean https;

		private final Supplier<String> resolver;

		private DefaultBaseUrl(boolean https, Supplier<String> resolver) {
			Assert.notNull(resolver, "'resolver' must not be null");
			this.https = https;
			this.resolver = resolver;
		}

		@Override
		public boolean isHttps() {
			return this.https;
		}

		@Override
		public UriBuilderFactory getUriBuilderFactory() {
			return new DefaultUriBuilderFactory(resolve());
		}

		String resolve() {
			return this.resolver.get();
		}

		@Override
		public BaseUrl withPath(String path) {
			Supplier<String> updatedResolver = () -> this.resolver.get() + path;
			return new DefaultBaseUrl(this.https, updatedResolver);
		}

	}

}
