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

package org.springframework.boot.test.http.client;

import java.net.URI;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.test.http.server.BaseUrl;
import org.springframework.util.Assert;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * {@link UriBuilderFactory} to support {@link BaseUrl}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public final class BaseUrlUriBuilderFactory implements UriBuilderFactory {

	private final BaseUrl baseUrl;

	/**
	 * Create a new {@link BaseUrlUriBuilderFactory} instance.
	 * @param baseUrl the base URL to use
	 */
	BaseUrlUriBuilderFactory(BaseUrl baseUrl) {
		Assert.notNull(baseUrl, "'baseUrl' must not be null");
		this.baseUrl = baseUrl;
	}

	/**
	 * Get a {@link UriBuilderFactory} instance applying the given {@code baseUrl}.
	 * @param baseUrl the base URL to apply or {@code null}
	 * @return a factory for the given base URL
	 */
	public static UriBuilderFactory get(@Nullable BaseUrl baseUrl) {
		return (baseUrl != null) ? new BaseUrlUriBuilderFactory(baseUrl) : new DefaultUriBuilderFactory();
	}

	@Override
	public UriBuilder uriString(String uriTemplate) {
		return createDelegate().uriString(uriTemplate);
	}

	@Override
	public UriBuilder builder() {
		return UriComponentsBuilder.newInstance();
	}

	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		return createDelegate().expand(uriTemplate, uriVariables);
	}

	@Override
	public URI expand(String uriTemplate, @Nullable Object... uriVariables) {
		return createDelegate().expand(uriTemplate, uriVariables);
	}

	private UriBuilderFactory createDelegate() {
		return this.baseUrl.getUriBuilderFactory();
	}

}
