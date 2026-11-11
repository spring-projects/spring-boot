/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import org.jspecify.annotations.Nullable;
import reactor.netty.transport.ClientTransport.ResolvedAddressSelector;

import org.springframework.util.CollectionUtils;

/**
 * Reactor Netty {@link ResolvedAddressSelector} that filters using a
 * {@link InetAddressFilter}.
 *
 * @param <C> the client configuration implementation
 * @author Phillip Webb
 */
class ReactorFilteredResolvedAddressSelector<C> implements ResolvedAddressSelector<C> {

	private final @Nullable ResolvedAddressSelector<? super C> delegate;

	private final InetAddressFilter filter;

	ReactorFilteredResolvedAddressSelector(@Nullable ResolvedAddressSelector<? super C> delegate,
			InetAddressFilter filter) {
		this.delegate = delegate;
		this.filter = filter;
	}

	@Override
	public @Nullable List<? extends SocketAddress> apply(C config, List<? extends SocketAddress> resolvedAddresses) {
		return filter((this.delegate != null) ? this.delegate.apply(config, resolvedAddresses) : resolvedAddresses);
	}

	private @Nullable List<? extends SocketAddress> filter(@Nullable List<? extends SocketAddress> resolvedAddresses) {
		if (CollectionUtils.isEmpty(resolvedAddresses)) {
			return resolvedAddresses;
		}
		return FilteredAddresses.of(resolvedAddresses.stream(), this::matches)
			.toList()
			.orElseThrow(() -> hostString(resolvedAddresses), this.filter);
	}

	private boolean matches(SocketAddress address) {
		return (address instanceof InetSocketAddress socketAddress) ? this.filter.matches(socketAddress) : true;
	}

	private String hostString(List<? extends SocketAddress> resolvedAddresses) {
		List<String> hosts = resolvedAddresses.stream()
			.filter(InetSocketAddress.class::isInstance)
			.map(InetSocketAddress.class::cast)
			.map(InetSocketAddress::getAddress)
			.map(InetAddress::getHostAddress)
			.toList();
		if (hosts.isEmpty()) {
			return "unknown";
		}
		if (hosts.size() == 1) {
			return hosts.get(0);
		}
		return hosts.toString();
	}

}
