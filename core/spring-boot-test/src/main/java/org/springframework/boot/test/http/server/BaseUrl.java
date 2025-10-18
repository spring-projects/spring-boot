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
import org.springframework.util.function.SingletonSupplier;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

/**
 * A base URL that can be used to connect to the running server.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public final class BaseUrl {

	/**
	 * {@link BaseUrl} that resolves to {@code http://localhost}.
	 */
	public static final BaseUrl LOCALHOST = BaseUrl.of("http://localhost");

	private final boolean https;

	private final Supplier<String> resolver;

	private BaseUrl(boolean https, Supplier<String> resolver) {
		this.https = https;
		this.resolver = SingletonSupplier.of(resolver);
	}

	/**
	 * Return if the URL will ultimately resolve to an HTTPS address.
	 * @return if the URL is HTTPS
	 */
	public boolean isHttps() {
		return this.https;
	}

	/**
	 * Get a {@link UriBuilderFactory} that applies the base URL.
	 * @return a {@link UriBuilderFactory}
	 */
	public UriBuilderFactory getUriBuilderFactory() {
		return new DefaultUriBuilderFactory(this.resolver.get());
	}

	/**
	 * Return a new instance that applies the given {@code path}.
	 * @param path a path to append
	 * @return a new instance with the path added
	 */
	public BaseUrl withPath(String path) {
		return new BaseUrl(this.https, () -> this.resolver.get() + path);
	}

	/**
	 * Factory method to create a new {@link BaseUrl}.
	 * @param url the URL to use
	 * @return a new {@link BaseUrl} instance
	 */
	public static BaseUrl of(String url) {
		Assert.notNull(url, "'url' must not be null");
		return of(StringUtils.startsWithIgnoreCase(url, "https"), () -> url);
	}

	/**
	 * Factory method to create a new {@link BaseUrl}.
	 * @param https whether the base URL is https
	 * @param resolver the resolver used to supply the actual URL
	 * @return a new {@link BaseUrl} instance
	 */
	public static BaseUrl of(boolean https, Supplier<String> resolver) {
		Assert.notNull(resolver, "'resolver' must not be null");
		return new BaseUrl(https, resolver);
	}

}
