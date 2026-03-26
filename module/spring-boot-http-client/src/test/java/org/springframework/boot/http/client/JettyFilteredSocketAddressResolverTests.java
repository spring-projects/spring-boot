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
import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyFilteredSocketAddressResolver}.
 *
 * @author Phillip Webb
 */
class JettyFilteredSocketAddressResolverTests {

	@Test
	void resolveWhenMatchedOnlyElement() {
		InetSocketAddress localhost = inetSocketAddress("localhost", 8080);
		SocketAddressResolver delegate = (host, port, context, promise) -> promise.succeeded(List.of(localhost));
		JettyFilteredSocketAddressResolver resolver = new JettyFilteredSocketAddressResolver(delegate,
				InetAddressFilter.internalAddresses());
		Promise<List<InetSocketAddress>> promise = mock();
		resolver.resolve("localhost", 8080, Collections.emptyMap(), promise);
		then(promise).should().succeeded(List.of(localhost));
	}

	@Test
	void resolveWhenMatchedOneOfManyElements() {
		InetSocketAddress localhost = inetSocketAddress("localhost", 8080);
		InetSocketAddress remote = inetSocketAddress("8.8.8.8", 8080);
		SocketAddressResolver delegate = (host, port, context, promise) -> promise
			.succeeded(List.of(localhost, remote));
		JettyFilteredSocketAddressResolver resolver = new JettyFilteredSocketAddressResolver(delegate,
				InetAddressFilter.internalAddresses());
		Promise<List<InetSocketAddress>> promise = mock();
		resolver.resolve("localhost", 8080, Collections.emptyMap(), promise);
		then(promise).should().succeeded(List.of(localhost));
	}

	@Test
	void resolveWhenMatchedNoElements() {
		InetSocketAddress localhost = inetSocketAddress("localhost", 8080);
		InetSocketAddress remote = inetSocketAddress("8.8.8.8", 8080);
		SocketAddressResolver delegate = (host, port, context, promise) -> promise
			.succeeded(List.of(localhost, remote));
		JettyFilteredSocketAddressResolver resolver = new JettyFilteredSocketAddressResolver(delegate,
				InetAddressFilter.externalAddresses().andNot("8.8.8.8"));
		Promise<List<InetSocketAddress>> promise = mock();
		resolver.resolve("localhost", 8080, Collections.emptyMap(), promise);
		ArgumentCaptor<Throwable> failure = ArgumentCaptor.captor();
		then(promise).should().failed(failure.capture());
		assertThat(failure.getValue()).isInstanceOf(FilteredHostException.class);
	}

	@Test
	void resolveWhenDelegateFails() {
		Throwable ex = new RuntimeException();
		SocketAddressResolver delegate = (host, port, context, promise) -> promise.failed(ex);
		JettyFilteredSocketAddressResolver resolver = new JettyFilteredSocketAddressResolver(delegate,
				InetAddressFilter.externalAddresses());
		Promise<List<InetSocketAddress>> promise = mock();
		resolver.resolve("localhost", 8080, Collections.emptyMap(), promise);
		then(promise).should().failed(ex);
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
