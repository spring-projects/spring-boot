/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * Adapts an {@link InetAddressFilter} to a variant that works with URLs.
 *
 * @author Phillip Webb
 */
class UrlHostFilter {

	private final InetAddressFilter filter;

	UrlHostFilter(InetAddressFilter filter) {
		this.filter = filter;
	}

	void throwIfFiltered(URI uri) {
		FilteredAddresses.of(Stream.of(uri), this::matchesUri).get().orElseThrow(uri.getHost(), this.filter);
	}

	private boolean matchesUri(URI uri) {
		String host = uri.getHost();
		int port = uri.getPort();
		InetAddress resolved = resolve(host);
		if (resolved != null && port > 0) {
			return this.filter.matches(new InetSocketAddress(resolved, uri.getPort()));
		}
		return (resolved != null) && this.filter.matches(resolved);
	}

	private @Nullable InetAddress resolve(String host) {
		try {
			// We follow the same resolution logic as
			// jdk.internal.net.http.HttpRequestImpl.getAddress()
			return InetAddress.getByName(host);
		}
		catch (UnknownHostException ex) {
			return null;
		}
	}

}
