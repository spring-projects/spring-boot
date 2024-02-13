/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.client;

import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplateHandler;

/**
 * {@link UriBuilderFactory} to set the root for URI that starts with {@code '/'}.
 *
 * @author Scott Frederick
 * @since 3.2.3
 */
public class RootUriBuilderFactory extends RootUriTemplateHandler implements UriBuilderFactory {

	@SuppressWarnings("removal")
	RootUriBuilderFactory(String rootUri) {
		super(rootUri);
	}

	@SuppressWarnings("removal")
	RootUriBuilderFactory(String rootUri, UriTemplateHandler delegate) {
		super(rootUri, delegate);
	}

	@Override
	public UriBuilder uriString(String uriTemplate) {
		return UriComponentsBuilder.fromUriString(apply(uriTemplate));
	}

	@Override
	public UriBuilder builder() {
		return UriComponentsBuilder.newInstance();
	}

	/**
	 * Apply a {@link RootUriBuilderFactory} instance to the given {@link RestTemplate}.
	 * @param restTemplate the {@link RestTemplate} to add the builder factory to
	 * @param rootUri the root URI
	 */
	static void applyTo(RestTemplate restTemplate, String rootUri) {
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		RootUriBuilderFactory handler = new RootUriBuilderFactory(rootUri, restTemplate.getUriTemplateHandler());
		restTemplate.setUriTemplateHandler(handler);
	}

}
