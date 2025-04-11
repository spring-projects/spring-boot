/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import io.netty.channel.ChannelOption;
import org.junit.jupiter.api.Test;
import reactor.netty.http.client.HttpClient;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ReactorHttpClientBuilder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactorClientHttpConnectorBuilder} and
 * {@link ReactorHttpClientBuilder}.
 *
 * @author Phillip Webb
 */
class ReactorClientHttpConnectorBuilderTests
		extends AbstractClientHttpConnectorBuilderTests<ReactorClientHttpConnector> {

	ReactorClientHttpConnectorBuilderTests() {
		super(ReactorClientHttpConnector.class, ClientHttpConnectorBuilder.reactor());
	}

	@Test
	void withCustomizers() {
		List<HttpClient> httpClients = new ArrayList<>();
		UnaryOperator<HttpClient> httpClientCustomizer1 = (httpClient) -> {
			httpClients.add(httpClient);
			return httpClient;
		};
		UnaryOperator<HttpClient> httpClientCustomizer2 = (httpClient) -> {
			httpClients.add(httpClient);
			return httpClient;
		};
		ClientHttpRequestFactoryBuilder.reactor()
			.withHttpClientCustomizer(httpClientCustomizer1)
			.withHttpClientCustomizer(httpClientCustomizer2)
			.build();
		assertThat(httpClients).hasSize(2);
	}

	@Override
	protected long connectTimeout(ReactorClientHttpConnector connector) {
		return (int) ((HttpClient) ReflectionTestUtils.getField(connector, "httpClient")).configuration()
			.options()
			.get(ChannelOption.CONNECT_TIMEOUT_MILLIS);
	}

	@Override
	protected long readTimeout(ReactorClientHttpConnector connector) {
		return (int) ((HttpClient) ReflectionTestUtils.getField(connector, "httpClient")).configuration()
			.responseTimeout()
			.toMillis();
	}

}
