/*
 * Copyright 2012-present the original author or authors.
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
