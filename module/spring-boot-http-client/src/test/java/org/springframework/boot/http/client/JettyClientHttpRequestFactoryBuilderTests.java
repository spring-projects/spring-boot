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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyClientHttpRequestFactoryBuilder} and
 * {@link JettyHttpClientBuilder}.
 *
 * @author Phillip Webb
 */

class JettyClientHttpRequestFactoryBuilderTests

		extends AbstractClientHttpRequestFactoryBuilderTests<JettyClientHttpRequestFactory> {

	JettyClientHttpRequestFactoryBuilderTests() {
		super(JettyClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.jetty());
	}

	@Test
	void withCustomizers() {
		TestCustomizer<HttpClient> httpClientCustomizer1 = new TestCustomizer<>();
		TestCustomizer<HttpClient> httpClientCustomizer2 = new TestCustomizer<>();
		TestCustomizer<HttpClientTransport> httpClientTransportCustomizer = new TestCustomizer<>();
		TestCustomizer<ClientConnector> clientConnectorCustomizerCustomizer = new TestCustomizer<>();
		ClientHttpRequestFactoryBuilder.jetty()
			.withHttpClientCustomizer(httpClientCustomizer1)
			.withHttpClientCustomizer(httpClientCustomizer2)
			.withHttpClientTransportCustomizer(httpClientTransportCustomizer)
			.withClientConnectorCustomizerCustomizer(clientConnectorCustomizerCustomizer)
			.build();
		httpClientCustomizer1.assertCalled();
		httpClientCustomizer2.assertCalled();
		httpClientTransportCustomizer.assertCalled();
		clientConnectorCustomizerCustomizer.assertCalled();
	}

	@Test
	void promiseClassIsAvailable() {
		Promise<List<java.net.InetSocketAddress>> promise = null;
	}

	@Test
	void withDnsResolver() throws Exception {
		// Create a mock DNS resolver to control its behavior and verify interactions.
		SocketAddressResolver dnsResolver = mock(SocketAddressResolver.class);

		// Define the mock's behavior: when 'resolve' is called, fulfill the promise with
		// a
		// successful result (a fake IP address).
		BDDMockito.willAnswer((invocation) -> {
			Promise<List<InetSocketAddress>> promise = invocation.getArgument(2);
			promise.succeeded(Collections.singletonList(new InetSocketAddress("127.0.0.1", 80)));
			return null;
		}).given(dnsResolver).resolve(eq("localhost"), eq(80), any());

		// Build the request factory, injecting the mock DNS resolver into its settings.
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
			.withDnsResolver(dnsResolver);
		JettyClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.jetty().build(settings);

		// Execute a request to trigger the DNS resolution process.
		// The actual success or failure of the request is not important for this test;
		// we only care that the DNS resolver is invoked.
		try {
			requestFactory.createRequest(new URI("http://localhost/test"), HttpMethod.GET).execute();
		}
		catch (Exception ex) {
			// Ignore any exceptions from the request execution.
		}

		// Verify that the mock DNS resolver's 'resolve' method was called with the
		// expected arguments.
		then(dnsResolver).should().resolve(eq("localhost"), eq(80), any());
	}

	@Override
	protected long connectTimeout(JettyClientHttpRequestFactory requestFactory) {
		return ((HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient")).getConnectTimeout();
	}

	@Override
	protected long readTimeout(JettyClientHttpRequestFactory requestFactory) {
		return (long) ReflectionTestUtils.getField(requestFactory, "readTimeout");
	}

}
