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
 * @since 4.0.0
 */
public class BaseUrlUriBuilderFactory implements UriBuilderFactory {

	private final UriBuilderFactory delegate;

	private final BaseUrl baseUrl;

	/**
	 * Create a new {@link BaseUrlUriBuilderFactory} instance.
	 * @param delegate the delegate {@link UriBuilderFactory}
	 * @param baseUrl the base URL to use
	 */
	public BaseUrlUriBuilderFactory(UriBuilderFactory delegate, BaseUrl baseUrl) {
		Assert.notNull(delegate, "'delegate' must not be null");
		Assert.notNull(baseUrl, "'baseUrl' must not be null");
		this.delegate = delegate;
		this.baseUrl = baseUrl;
	}

	@Override
	public UriBuilder uriString(String uriTemplate) {
		return UriComponentsBuilder.fromUriString(apply(uriTemplate));
	}

	@Override
	public UriBuilder builder() {
		return UriComponentsBuilder.newInstance();
	}

	@Override
	public URI expand(String uriTemplate, Map<String, ?> uriVariables) {
		return this.delegate.expand(apply(uriTemplate), uriVariables);
	}

	@Override
	public URI expand(String uriTemplate, @Nullable Object... uriVariables) {
		return this.delegate.expand(apply(uriTemplate), uriVariables);
	}

	String apply(String uriTemplate) {
		return (uriTemplate.startsWith("/")) ? this.baseUrl.resolve(uriTemplate) : uriTemplate;
	}

	public static UriBuilderFactory get(@Nullable BaseUrl baseUrl) {
		DefaultUriBuilderFactory delegate = new DefaultUriBuilderFactory();
		return (baseUrl != null) ? new BaseUrlUriBuilderFactory(delegate, baseUrl) : delegate;
	}

}
