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

import java.util.ArrayList;
import java.util.List;

import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.io.SocketConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.client.HttpComponentsHttpClientBuilder.TlsSocketStrategyFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpComponentsClientHttpRequestFactoryBuilder} and
 * {@link HttpComponentsHttpClientBuilder}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class HttpComponentsClientHttpRequestFactoryBuilderTests
		extends AbstractClientHttpRequestFactoryBuilderTests<HttpComponentsClientHttpRequestFactory> {

	HttpComponentsClientHttpRequestFactoryBuilderTests() {
		super(HttpComponentsClientHttpRequestFactory.class, ClientHttpRequestFactoryBuilder.httpComponents());
	}

	@Test
	void withCustomizers() {
		TestCustomizer<HttpClientBuilder> httpClientCustomizer1 = new TestCustomizer<>();
		TestCustomizer<HttpClientBuilder> httpClientCustomizer2 = new TestCustomizer<>();
		TestCustomizer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer = new TestCustomizer<>();
		TestCustomizer<SocketConfig.Builder> socketConfigCustomizer = new TestCustomizer<>();
		TestCustomizer<SocketConfig.Builder> socketConfigCustomizer1 = new TestCustomizer<>();
		TestCustomizer<RequestConfig.Builder> defaultRequestConfigCustomizer = new TestCustomizer<>();
		TestCustomizer<RequestConfig.Builder> defaultRequestConfigCustomizer1 = new TestCustomizer<>();
		ClientHttpRequestFactoryBuilder.httpComponents()
			.withHttpClientCustomizer(httpClientCustomizer1)
			.withHttpClientCustomizer(httpClientCustomizer2)
			.withConnectionManagerCustomizer(connectionManagerCustomizer)
			.withSocketConfigCustomizer(socketConfigCustomizer)
			.withSocketConfigCustomizer(socketConfigCustomizer1)
			.withDefaultRequestConfigCustomizer(defaultRequestConfigCustomizer)
			.withDefaultRequestConfigCustomizer(defaultRequestConfigCustomizer1)
			.build();
		httpClientCustomizer1.assertCalled();
		httpClientCustomizer2.assertCalled();
		connectionManagerCustomizer.assertCalled();
		socketConfigCustomizer.assertCalled();
		socketConfigCustomizer1.assertCalled();
		defaultRequestConfigCustomizer.assertCalled();
		defaultRequestConfigCustomizer1.assertCalled();
	}

	@Test
	@WithPackageResources("test.jks")
	void withTlsSocketStrategyFactory() {
		HttpClientSettings settings = HttpClientSettings.ofSslBundle(sslBundle());
		List<SslBundle> bundles = new ArrayList<>();
		TlsSocketStrategyFactory tlsSocketStrategyFactory = (bundle) -> {
			bundles.add(bundle);
			return (socket, target, port, attachment, context) -> null;
		};
		ClientHttpRequestFactoryBuilder.httpComponents()
			.withTlsSocketStrategyFactory(tlsSocketStrategyFactory)
			.build(settings);
		assertThat(bundles).contains(settings.sslBundle());
	}

	@Test
	void with() {
		TestCustomizer<HttpClientBuilder> customizer = new TestCustomizer<>();
		ClientHttpRequestFactoryBuilder.httpComponents()
			.with((builder) -> builder.withHttpClientCustomizer(customizer))
			.build();
		customizer.assertCalled();
	}

	@Override
	protected long connectTimeout(HttpComponentsClientHttpRequestFactory requestFactory) {
		Object field = ReflectionTestUtils.getField(requestFactory, "connectTimeout");
		assertThat(field).isNotNull();
		return (long) field;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected long readTimeout(HttpComponentsClientHttpRequestFactory requestFactory) {
		HttpClient httpClient = requestFactory.getHttpClient();
		Object connectionManager = ReflectionTestUtils.getField(httpClient, "connManager");
		assertThat(connectionManager).isNotNull();
		Resolver<HttpRoute, SocketConfig> socketConfigResolver = (Resolver<HttpRoute, SocketConfig>) ReflectionTestUtils
			.getField(connectionManager, "socketConfigResolver");
		assertThat(socketConfigResolver).isNotNull();
		SocketConfig socketConfig = socketConfigResolver.resolve(null);
		return socketConfig.getSoTimeout().toMilliseconds();
	}

}
