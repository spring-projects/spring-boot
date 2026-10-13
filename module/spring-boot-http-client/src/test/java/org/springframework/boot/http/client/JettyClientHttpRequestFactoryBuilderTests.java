/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.client;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

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
	void with() {
		TestCustomizer<HttpClient> customizer = new TestCustomizer<>();
		ClientHttpRequestFactoryBuilder.jetty().with((builder) -> builder.withHttpClientCustomizer(customizer)).build();
		customizer.assertCalled();
	}

	@Test
	void withHttpClientTransportFactory() {
		JettyClientHttpRequestFactory factory = ClientHttpRequestFactoryBuilder.jetty()
			.withHttpClientTransportFactory(TestHttpClientTransport::new)
			.build();
		assertThat(factory).extracting("httpClient")
			.extracting("transport")
			.isInstanceOf(TestHttpClientTransport.class);
	}

	@Override
	protected long connectTimeout(JettyClientHttpRequestFactory requestFactory) {
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient");
		assertThat(httpClient).isNotNull();
		return httpClient.getConnectTimeout();
	}

	@Override
	protected long readTimeout(JettyClientHttpRequestFactory requestFactory) {
		Object field = ReflectionTestUtils.getField(requestFactory, "readTimeout");
		assertThat(field).isNotNull();
		return (long) field;
	}

	static class TestHttpClientTransport extends HttpClientTransportOverHTTP {

		TestHttpClientTransport(ClientConnector connector) {
			super(connector);
		}

	}

}
