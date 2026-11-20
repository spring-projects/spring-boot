/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

/**
 * JDK {@link ProxySelector} to check a URL is not filtered by a
 * {@link InetAddressFilter}.
 *
 * @author Phillip Webb
 */
class JdkFilteredProxySelector extends ProxySelector {

	private final ProxySelector delegate;

	private final UrlHostFilter urlHostFilter;

	JdkFilteredProxySelector(ProxySelector delegate, UrlHostFilter urlHostFilter) {
		this.delegate = delegate;
		this.urlHostFilter = urlHostFilter;
	}

	@Override
	public List<Proxy> select(URI uri) {
		this.urlHostFilter.throwIfFiltered(uri);
		return this.delegate.select(uri);
	}

	@Override
	public void connectFailed(URI uri, SocketAddress address, IOException ex) {
		this.delegate.connectFailed(uri, address, ex);
	}

}
