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

package org.springframework.boot.http.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

/**
 * JDK {@link ProxySelector} to check a URL is not filtered by a
 * {@link InetAddressFilter}.
 *
 * @author Phillip Webb
 */
class JdkFilteredProxySelector extends ProxySelector {

	private final ProxySelector delegate;

	private final InetAddressFilter filter;

	JdkFilteredProxySelector(ProxySelector delegate, InetAddressFilter filter) {
		this.delegate = delegate;
		this.filter = filter;
	}

	@Override
	public List<Proxy> select(URI uri) {
		String host = uri.getHost();
		FilteredAddresses.of(Stream.of(host), this::matchesResolvedHost).get().orElseThrow(host, this.filter);
		return this.delegate.select(uri);
	}

	private boolean matchesResolvedHost(String host) {
		InetAddress resolved = resolve(host);
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

	@Override
	public void connectFailed(URI uri, SocketAddress address, IOException ex) {
		this.delegate.connectFailed(uri, address, ex);
	}

}
