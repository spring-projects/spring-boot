/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;

/**
 * Jetty {@link SocketAddressResolver} that filters using a {@link InetAddressFilter}.
 *
 * @author Phillip Webb
 */
class JettyFilteredSocketAddressResolver implements SocketAddressResolver {

	private final SocketAddressResolver delegate;

	private final InetAddressFilter filter;

	JettyFilteredSocketAddressResolver(SocketAddressResolver delegate, InetAddressFilter filter) {
		this.delegate = delegate;
		this.filter = filter;
	}

	@Override
	public void resolve(String host, int port, Map<String, Object> context, Promise<List<InetSocketAddress>> promise) {
		this.delegate.resolve(host, port, context, new FilteredPromise(host, promise));
	}

	class FilteredPromise implements Promise<List<InetSocketAddress>> {

		private final String host;

		private final Promise<List<InetSocketAddress>> delegate;

		FilteredPromise(String host, Promise<List<InetSocketAddress>> delegate) {
			this.host = host;
			this.delegate = delegate;
		}

		@Override
		public void succeeded(List<InetSocketAddress> result) {
			try {
				this.delegate.succeeded(filter(result));
			}
			catch (FilteredHostException ex) {
				failed(ex);
			}
		}

		private List<InetSocketAddress> filter(List<InetSocketAddress> result) {
			InetAddressFilter filter = JettyFilteredSocketAddressResolver.this.filter;
			return FilteredAddresses.of(result.stream(), filter::matches).toList().orElseThrow(this.host, filter);
		}

		@Override
		public void failed(Throwable ex) {
			this.delegate.failed(ex);
		}

	}

}
