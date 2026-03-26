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
