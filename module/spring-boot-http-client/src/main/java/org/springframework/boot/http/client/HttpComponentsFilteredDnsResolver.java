/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.hc.client5.http.DnsResolver;

import org.springframework.util.ObjectUtils;

/**
 * HTTP Components {@link DnsResolver} that filters using a {@link InetAddressFilter}.
 *
 * @author Phillip Webb
 */
class HttpComponentsFilteredDnsResolver implements DnsResolver {

	private final DnsResolver delegate;

	private final InetAddressFilter filter;

	HttpComponentsFilteredDnsResolver(DnsResolver delegate, InetAddressFilter filter) {
		this.delegate = delegate;
		this.filter = filter;
	}

	@Override
	public InetAddress[] resolve(String host) throws UnknownHostException {
		InetAddress[] resolved = this.delegate.resolve(host);
		if (ObjectUtils.isEmpty(resolved)) {
			return resolved;
		}
		return FilteredAddresses.of(Arrays.stream(resolved), this.filter::matches)
			.toArray(InetAddress[]::new)
			.orElseThrow(host, this.filter);
	}

	@Override
	public List<InetSocketAddress> resolve(String host, int port) throws UnknownHostException {
		List<InetSocketAddress> resolved = this.delegate.resolve(host, port);
		if (resolved.isEmpty()) {
			return resolved;
		}
		return FilteredAddresses.of(resolved.stream(), this.filter::matches).toList().orElseThrow(host, this.filter);
	}

	@Override
	public String resolveCanonicalHostname(String host) throws UnknownHostException {
		return this.delegate.resolveCanonicalHostname(host);
	}

}
