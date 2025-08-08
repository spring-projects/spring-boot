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
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;

public class BannedHostDnsResolver implements DnsResolver {

	private final String bannedHost;

	private final DnsResolver defaultDnsResolver = new SystemDefaultDnsResolver();

	public BannedHostDnsResolver(String bannedHost) {
		this.bannedHost = bannedHost;
	}

	@Override
	public InetAddress[] resolve(String host) throws UnknownHostException {
		if (host.equals(bannedHost)) {
			throw new UnknownHostException(host);
		}
		return this.defaultDnsResolver.resolve(host);
	}

	@Override
	public String resolveCanonicalHostname(String host) throws UnknownHostException {
		return this.defaultDnsResolver.resolveCanonicalHostname(host);
	}
}
