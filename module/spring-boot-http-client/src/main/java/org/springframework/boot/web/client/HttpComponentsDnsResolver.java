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

package org.springframework.boot.web.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

/**
 * {@link DnsResolver} implementation that delegates to a {@link DnsResolver} and filters
 * the results using a {@link SecurityDnsHandler}.
 *
 * @author Scott Frederick
 * @since 3.2.0
 */
public class HttpComponentsDnsResolver implements DnsResolver {

	private final DnsResolver delegate;

	private final SecurityDnsHandler securityDnsHandler;

	public HttpComponentsDnsResolver(DnsResolver delegate, SecurityDnsHandler securityDnsHandler) {
		this.delegate = delegate;
		this.securityDnsHandler = securityDnsHandler;
	}

	public HttpComponentsDnsResolver(SecurityDnsHandler securityDnsHandler) {
		this(SystemDefaultDnsResolver.INSTANCE, securityDnsHandler);
	}

	@Override
	public InetAddress[] resolve(String host) throws UnknownHostException {
		InetAddress[] addresses = this.delegate.resolve(host);
		List<InetAddress> inetAddresses = this.securityDnsHandler.handleAddresses(Arrays.asList(addresses));
		return inetAddresses.toArray(new InetAddress[0]);
	}

	@Override
	public String resolveCanonicalHostname(String host) throws UnknownHostException {
		return this.delegate.resolveCanonicalHostname(host);
	}

}
