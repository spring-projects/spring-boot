/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient;

import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriTemplateHandler;

/**
 * {@link UriBuilderFactory} to set the root for URI that starts with {@code '/'}.
 *
 * @author Scott Frederick
 * @since 4.0.0
 * @deprecated since 4.1.0 for removal in 4.3.0 in favor of
 * {@link DefaultUriBuilderFactory}.
 */
@Deprecated(forRemoval = true, since = "4.1.0")
@SuppressWarnings("removal")
public class RootUriBuilderFactory extends RootUriTemplateHandler implements UriBuilderFactory {

	/**
	 * Create an instance with the root URI to use.
	 * @param rootUri the root URI
	 * @param delegate the {@link UriTemplateHandler} to delegate to
	 */
	public RootUriBuilderFactory(String rootUri, UriTemplateHandler delegate) {
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
		Assert.notNull(restTemplate, "'restTemplate' must not be null");
		RootUriBuilderFactory handler = new RootUriBuilderFactory(rootUri, restTemplate.getUriTemplateHandler());
		restTemplate.setUriTemplateHandler(handler);
	}

}
