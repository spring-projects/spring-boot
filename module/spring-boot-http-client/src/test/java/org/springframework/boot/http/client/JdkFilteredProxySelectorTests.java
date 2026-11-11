/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdkFilteredProxySelector}.
 *
 * @author Phillip Webb
 */
class JdkFilteredProxySelectorTests {

	@Test
	void selectWhenMatchesResolvedHost() throws Exception {
		URI localhost = new URI("http://localhost");
		ProxySelector delegate = mock(ProxySelector.class);
		Proxy proxy = mock();
		given(delegate.select(localhost)).willReturn(List.of(proxy));
		JdkFilteredProxySelector proxySelector = new JdkFilteredProxySelector(delegate,
				InetAddressFilter.internalAddresses());
		assertThat(proxySelector.select(localhost)).containsExactly(proxy);
	}

	@Test
	void selectWhenDoesNotMatchResolvedHost() throws Exception {
		URI localhost = new URI("http://localhost");
		ProxySelector delegate = mock(ProxySelector.class);
		Proxy proxy = mock();
		given(delegate.select(localhost)).willReturn(List.of(proxy));
		JdkFilteredProxySelector proxySelector = new JdkFilteredProxySelector(delegate,
				InetAddressFilter.externalAddresses());
		assertThatExceptionOfType(FilteredHostException.class).isThrownBy(() -> proxySelector.select(localhost));
	}

	@Test
	void connectFailDelegates() throws Exception {
		URI localhost = new URI("http://localhost");
		ProxySelector delegate = mock(ProxySelector.class);
		JdkFilteredProxySelector proxySelector = new JdkFilteredProxySelector(delegate,
				InetAddressFilter.externalAddresses());
		SocketAddress address = mock();
		IOException ex = new IOException();
		proxySelector.connectFailed(localhost, address, ex);
		then(delegate).should().connectFailed(localhost, address, ex);
	}

}
