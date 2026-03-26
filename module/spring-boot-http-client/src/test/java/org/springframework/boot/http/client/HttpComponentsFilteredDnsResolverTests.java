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
import java.util.List;

import org.apache.hc.client5.http.DnsResolver;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpComponentsFilteredDnsResolver}.
 *
 * @author Phillip Webb
 */
class HttpComponentsFilteredDnsResolverTests {

	@Test
	void resolveHostWhenMatchedOnlyElement() throws Exception {
		DnsResolver delegate = mock();
		InetAddress localhost = InetAddress.getLocalHost();
		given(delegate.resolve("localhost")).willReturn(new InetAddress[] { localhost });
		HttpComponentsFilteredDnsResolver dnsResolver = new HttpComponentsFilteredDnsResolver(delegate,
				InetAddressFilter.internalAddresses());
		assertThat(dnsResolver.resolve("localhost")).containsExactly(localhost);
	}

	@Test
	void resolveHostWhenMatchedOneOfManyElements() throws Exception {
		DnsResolver delegate = mock();
		InetAddress localhost = InetAddress.getLocalHost();
		InetAddress remote = InetAddress.getByName("8.8.8.8");
		given(delegate.resolve("localhost")).willReturn(new InetAddress[] { localhost, remote });
		HttpComponentsFilteredDnsResolver dnsResolver = new HttpComponentsFilteredDnsResolver(delegate,
				InetAddressFilter.internalAddresses());
		assertThat(dnsResolver.resolve("localhost")).containsExactly(localhost);
	}

	@Test
	void resolveHostWhenMatchedNoElements() throws UnknownHostException {
		DnsResolver delegate = mock();
		InetAddress localhost = InetAddress.getLocalHost();
		InetAddress remote = InetAddress.getByName("8.8.8.8");
		given(delegate.resolve("localhost")).willReturn(new InetAddress[] { localhost, remote });
		HttpComponentsFilteredDnsResolver dnsResolver = new HttpComponentsFilteredDnsResolver(delegate,
				InetAddressFilter.externalAddresses().andNot("8.8.8.8"));
		assertThatExceptionOfType(FilteredHostException.class).isThrownBy(() -> dnsResolver.resolve("localhost"));
	}

	@Test
	void resolveHostAndPortWhenMatchedOnlyElement() throws Exception {
		DnsResolver delegate = mock();
		InetAddress localhost = InetAddress.getLocalHost();
		given(delegate.resolve("localhost", 8080)).willReturn(List.of(new InetSocketAddress(localhost, 8080)));
		HttpComponentsFilteredDnsResolver dnsResolver = new HttpComponentsFilteredDnsResolver(delegate,
				InetAddressFilter.internalAddresses());
		assertThat(dnsResolver.resolve("localhost", 8080)).containsExactly(new InetSocketAddress(localhost, 8080));
	}

	@Test
	void resolveHostAndPortWhenMatchedOneOfManyElements() throws Exception {
		DnsResolver delegate = mock();
		InetAddress localhost = InetAddress.getLocalHost();
		InetAddress remote = InetAddress.getByName("8.8.8.8");
		given(delegate.resolve("localhost", 8080))
			.willReturn(List.of(new InetSocketAddress(localhost, 8080), new InetSocketAddress(remote, 8080)));
		HttpComponentsFilteredDnsResolver dnsResolver = new HttpComponentsFilteredDnsResolver(delegate,
				InetAddressFilter.internalAddresses());
		assertThat(dnsResolver.resolve("localhost", 8080)).containsExactly(new InetSocketAddress(localhost, 8080));
	}

	@Test
	void resolveHostAndPortWhenMatchedNoElements() throws UnknownHostException {
		DnsResolver delegate = mock();
		InetAddress localhost = InetAddress.getLocalHost();
		InetAddress remote = InetAddress.getByName("8.8.8.8");
		given(delegate.resolve("localhost", 8080))
			.willReturn(List.of(new InetSocketAddress(localhost, 8080), new InetSocketAddress(remote, 8080)));
		HttpComponentsFilteredDnsResolver dnsResolver = new HttpComponentsFilteredDnsResolver(delegate,
				InetAddressFilter.externalAddresses().andNot("8.8.8.8"));
		assertThatExceptionOfType(FilteredHostException.class).isThrownBy(() -> dnsResolver.resolve("localhost", 8080));
	}

	@Test
	void resolveCanonicalHostnameDelegates() throws Exception {
		DnsResolver delegate = mock();
		given(delegate.resolveCanonicalHostname("spring")).willReturn("boot");
		HttpComponentsFilteredDnsResolver dnsResolver = new HttpComponentsFilteredDnsResolver(delegate,
				InetAddressFilter.internalAddresses());
		assertThat(dnsResolver.resolveCanonicalHostname("spring")).isEqualTo("boot");
	}

}
