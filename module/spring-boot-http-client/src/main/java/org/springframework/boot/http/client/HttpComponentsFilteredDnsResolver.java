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
