/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * JDK {@link CookieHandler} to check a URL is not filtered by a
 * {@link InetAddressFilter}.
 *
 * @author Phillip Webb
 */
class JdkFilteredCookieHandler extends CookieHandler {

	private final @Nullable CookieHandler delegate;

	private final UrlHostFilter urlHostFilter;

	JdkFilteredCookieHandler(@Nullable CookieHandler delegate, UrlHostFilter urlHostFilter) {
		this.delegate = delegate;
		this.urlHostFilter = urlHostFilter;
	}

	@Override
	public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
		this.urlHostFilter.throwIfFiltered(uri);
		return (this.delegate != null) ? this.delegate.get(uri, requestHeaders) : Collections.emptyMap();

	}

	@Override
	public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
		if (this.delegate != null) {
			this.delegate.put(uri, responseHeaders);
		}
	}

}
