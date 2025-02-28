/*
 * Copyright 2012-2024 the original author or authors.
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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.io.ClientConnector;
import org.junit.jupiter.api.Test;

import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link JettyClientHttpRequestFactoryBuilder}.
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

	@Override
	protected long connectTimeout(JettyClientHttpRequestFactory requestFactory) {
		return ((HttpClient) ReflectionTestUtils.getField(requestFactory, "httpClient")).getConnectTimeout();
	}

	@Override
	protected long readTimeout(JettyClientHttpRequestFactory requestFactory) {
		return (long) ReflectionTestUtils.getField(requestFactory, "readTimeout");
	}

}
