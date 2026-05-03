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
