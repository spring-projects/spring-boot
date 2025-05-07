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

import java.net.http.HttpClient;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.JdkHttpClientBuilder;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for {@link JdkClientHttpConnectorBuilder} and {@link JdkHttpClientBuilder}.
 *
 * @author Phillip Webb
 */
class JdkClientHttpConnectorBuilderTests extends AbstractClientHttpConnectorBuilderTests<JdkClientHttpConnector> {

	JdkClientHttpConnectorBuilderTests() {
		super(JdkClientHttpConnector.class, ClientHttpConnectorBuilder.jdk());
	}

	@Test
	void withCustomizers() {
		TestCustomizer<HttpClient.Builder> httpClientCustomizer1 = new TestCustomizer<>();
		TestCustomizer<HttpClient.Builder> httpClientCustomizer2 = new TestCustomizer<>();
		ClientHttpRequestFactoryBuilder.jdk()
			.withHttpClientCustomizer(httpClientCustomizer1)
			.withHttpClientCustomizer(httpClientCustomizer2)
			.build();
		httpClientCustomizer1.assertCalled();
		httpClientCustomizer2.assertCalled();
	}

	@Override
	protected long connectTimeout(JdkClientHttpConnector connector) {
		HttpClient httpClient = (HttpClient) ReflectionTestUtils.getField(connector, "httpClient");
		return httpClient.connectTimeout().get().toMillis();
	}

	@Override
	protected long readTimeout(JdkClientHttpConnector connector) {
		Duration readTimeout = (Duration) ReflectionTestUtils.getField(connector, "readTimeout");
		return readTimeout.toMillis();
	}

}
