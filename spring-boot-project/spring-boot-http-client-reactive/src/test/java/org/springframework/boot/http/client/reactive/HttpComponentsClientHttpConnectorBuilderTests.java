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
import java.util.function.Function;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.HttpComponentsHttpAsyncClientBuilder;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpComponentsClientHttpConnectorBuilder} and
 * {@link HttpComponentsHttpAsyncClientBuilder}.
 *
 * @author Phillip Webb
 */
class HttpComponentsClientHttpConnectorBuilderTests
		extends AbstractClientHttpConnectorBuilderTests<HttpComponentsClientHttpConnector> {

	HttpComponentsClientHttpConnectorBuilderTests() {
		super(HttpComponentsClientHttpConnector.class, ClientHttpConnectorBuilder.httpComponents());
	}

	@Test
	void withCustomizers() {
		TestCustomizer<HttpAsyncClientBuilder> httpClientCustomizer1 = new TestCustomizer<>();
		TestCustomizer<HttpAsyncClientBuilder> httpClientCustomizer2 = new TestCustomizer<>();
		TestCustomizer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer = new TestCustomizer<>();
		TestCustomizer<ConnectionConfig.Builder> connectionConfigCustomizer1 = new TestCustomizer<>();
		TestCustomizer<ConnectionConfig.Builder> connectionConfigCustomizer2 = new TestCustomizer<>();
		TestCustomizer<RequestConfig.Builder> defaultRequestConfigCustomizer = new TestCustomizer<>();
		TestCustomizer<RequestConfig.Builder> defaultRequestConfigCustomizer1 = new TestCustomizer<>();
		ClientHttpConnectorBuilder.httpComponents()
			.withHttpClientCustomizer(httpClientCustomizer1)
			.withHttpClientCustomizer(httpClientCustomizer2)
			.withConnectionManagerCustomizer(connectionManagerCustomizer)
			.withConnectionConfigCustomizer(connectionConfigCustomizer1)
			.withConnectionConfigCustomizer(connectionConfigCustomizer2)
			.withDefaultRequestConfigCustomizer(defaultRequestConfigCustomizer)
			.withDefaultRequestConfigCustomizer(defaultRequestConfigCustomizer1)
			.build();
		httpClientCustomizer1.assertCalled();
		httpClientCustomizer2.assertCalled();
		connectionManagerCustomizer.assertCalled();
		connectionConfigCustomizer1.assertCalled();
		connectionConfigCustomizer2.assertCalled();
		defaultRequestConfigCustomizer.assertCalled();
		defaultRequestConfigCustomizer1.assertCalled();
	}

	@Test
	@WithPackageResources("test.jks")
	void withTlsSocketStrategyFactory() {
		ClientHttpConnectorSettings settings = ClientHttpConnectorSettings.ofSslBundle(sslBundle());
		List<SslBundle> bundles = new ArrayList<>();
		Function<SslBundle, TlsStrategy> tlsSocketStrategyFactory = (bundle) -> {
			bundles.add(bundle);
			return (sessionLayer, host, localAddress, remoteAddress, attachment, handshakeTimeout) -> false;
		};
		ClientHttpConnectorBuilder.httpComponents()
			.withTlsSocketStrategyFactory(tlsSocketStrategyFactory)
			.build(settings);
		assertThat(bundles).contains(settings.sslBundle());
	}

	@Override
	protected long connectTimeout(HttpComponentsClientHttpConnector connector) {
		return getConnectorConfig(connector).getConnectTimeout().toMilliseconds();
	}

	@Override
	protected long readTimeout(HttpComponentsClientHttpConnector connector) {
		return getConnectorConfig(connector).getSocketTimeout().toMilliseconds();
	}

	@SuppressWarnings("unchecked")
	private ConnectionConfig getConnectorConfig(HttpComponentsClientHttpConnector connector) {
		HttpAsyncClient httpClient = (HttpAsyncClient) ReflectionTestUtils.getField(connector, "client");
		Object manager = ReflectionTestUtils.getField(httpClient, "manager");
		ConnectionConfig connectorConfig = ((Resolver<HttpRoute, ConnectionConfig>) ReflectionTestUtils
			.getField(manager, "connectionConfigResolver")).resolve(null);
		return connectorConfig;
	}

}
