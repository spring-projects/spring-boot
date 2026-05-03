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

import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClientConfig;
import reactor.netty.transport.ClientTransport.ResolvedAddressSelector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactorFilteredResolvedAddressSelector}.
 *
 * @author Phillip Webb
 */
class ReactorFilteredResolvedAddressSelectorTests {

	@Test
	void applyWhenMatchedOnlyElement() {
		InetSocketAddress localhost = inetSocketAddress("localhost", 8080);
		ResolvedAddressSelector<HttpClientConfig> delegate = (config, resolvedAddresses) -> resolvedAddresses;
		HttpClientConfig config = mock();
		ReactorFilteredResolvedAddressSelector<HttpClientConfig> addressSelector = new ReactorFilteredResolvedAddressSelector<>(
				delegate, InetAddressFilter.internalAddresses());
		assertThat(addressSelector.apply(config, List.of(localhost))).isEqualTo(List.of(localhost));
	}

	@Test
	void applyWhenMatchedOneOfManyElements() {
		InetSocketAddress localhost = inetSocketAddress("localhost", 8080);
		InetSocketAddress remote = inetSocketAddress("8.8.8.8", 8080);
		ResolvedAddressSelector<HttpClientConfig> delegate = (config, resolvedAddresses) -> resolvedAddresses;
		HttpClientConfig config = mock();
		ReactorFilteredResolvedAddressSelector<HttpClientConfig> addressSelector = new ReactorFilteredResolvedAddressSelector<>(
				delegate, InetAddressFilter.internalAddresses());
		assertThat(addressSelector.apply(config, List.of(localhost, remote))).isEqualTo(List.of(localhost));
	}

	@Test
	void applyWhenMatchedNoElements() {
		InetSocketAddress localhost = inetSocketAddress("localhost", 8080);
		InetSocketAddress remote = inetSocketAddress("8.8.8.8", 8080);
		ResolvedAddressSelector<HttpClientConfig> delegate = (config, resolvedAddresses) -> resolvedAddresses;
		HttpClientConfig config = mock();
		ReactorFilteredResolvedAddressSelector<HttpClientConfig> addressSelector = new ReactorFilteredResolvedAddressSelector<>(
				delegate, InetAddressFilter.externalAddresses().andNot("8.8.8.8"));
		assertThatExceptionOfType(FilteredHostException.class)
			.isThrownBy(() -> addressSelector.apply(config, List.of(localhost, remote)))
			.withMessage("Filtered host '[127.0.0.1, 8.8.8.8]'");

	}

	private InetSocketAddress inetSocketAddress(String host, int port) {
		try {
			return new InetSocketAddress(InetAddress.getByName(host), port);
		}
		catch (UnknownHostException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
