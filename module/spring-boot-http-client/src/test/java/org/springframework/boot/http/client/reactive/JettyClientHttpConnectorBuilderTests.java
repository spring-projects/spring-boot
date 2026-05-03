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

package org.springframework.boot.http.client.reactive;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.InetAddressFilter;
import org.springframework.boot.http.client.JettyHttpClientBuilder;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyClientHttpConnectorBuilder} and {@link JettyHttpClientBuilder}.
 *
 * @author Phillip Webb
 */
class JettyClientHttpConnectorBuilderTests extends AbstractClientHttpConnectorBuilderTests<JettyClientHttpConnector> {

	JettyClientHttpConnectorBuilderTests() {
		super(JettyClientHttpConnector.class, ClientHttpConnectorBuilder.jetty());
	}

	@Test
	void withCustomizers() {
		TestCustomizer<HttpClient> httpClientCustomizer1 = new TestCustomizer<>();
		TestCustomizer<HttpClient> httpClientCustomizer2 = new TestCustomizer<>();
		TestCustomizer<HttpClientTransport> httpClientTransportCustomizer = new TestCustomizer<>();
		TestCustomizer<ClientConnector> clientConnectorCustomizerCustomizer = new TestCustomizer<>();
		ClientHttpConnectorBuilder.jetty()
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
	void with() {
		TestCustomizer<HttpClient> customizer = new TestCustomizer<>();
		ClientHttpConnectorBuilder.jetty().with((builder) -> builder.withHttpClientCustomizer(customizer)).build();
		customizer.assertCalled();
	}

	@Test
	void withHttpClientTransportFactory() {
		JettyClientHttpConnector connector = ClientHttpConnectorBuilder.jetty()
			.withHttpClientTransportFactory(TestHttpClientTransport::new)
			.build();
		assertThat(connector).extracting("httpClient")
			.extracting("transport")
			.isInstanceOf(TestHttpClientTransport.class);
	}

	@Test
	void withSocketAddressResolver() {
		SocketAddressResolver socketAddressResolver = mock();
		JettyClientHttpConnector connector = ClientHttpConnectorBuilder.jetty()
			.withSocketAddressResolver(socketAddressResolver)
			.build();
		assertThat(connector).extracting("httpClient.resolver").isSameAs(socketAddressResolver);
	}

	@Test
	void withSocketAddressResolverWhenHasInetAddressMatcher() {
		SocketAddressResolver socketAddressResolver = mock();
		JettyClientHttpConnector connector = ClientHttpConnectorBuilder.jetty()
			.withSocketAddressResolver(socketAddressResolver)
			.build(HttpClientSettings.defaults().withInetAddressFilter(InetAddressFilter.externalAddresses()));
		assertThat(connector).extracting("httpClient.resolver")
			.matches((resolver) -> resolver.getClass().getName().contains("JettyFiltered"));
		assertThat(connector).extracting("httpClient.resolver.delegate").isSameAs(socketAddressResolver);
	}

	@Override
	protected long connectTimeout(JettyClientHttpConnector connector) {
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
		assertThat(httpClient).isNotNull();
		return httpClient.getConnectTimeout();
	}

	@Override
	protected long readTimeout(JettyClientHttpConnector connector) {
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
		assertThat(httpClient).isNotNull();
		HttpClientSettings settings = (HttpClientSettings) ReflectionTestUtils.getField(httpClient, "settings");
		assertThat(settings).isNotNull();
		assertThat(settings.readTimeout()).isNotNull();
		return settings.readTimeout().toMillis();
	}

	static class TestHttpClientTransport extends HttpClientTransportOverHTTP {

		TestHttpClientTransport(ClientConnector connector) {
			super(connector);
		}

	}

}
