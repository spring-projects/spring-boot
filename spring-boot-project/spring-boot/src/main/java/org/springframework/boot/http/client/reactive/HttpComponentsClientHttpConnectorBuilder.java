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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;

import org.springframework.boot.http.client.HttpComponentsHttpAsyncClientBuilder;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpConnectorBuilder#httpComponents()}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public final class HttpComponentsClientHttpConnectorBuilder
		extends AbstractClientHttpConnectorBuilder<HttpComponentsClientHttpConnector> {

	private final HttpComponentsHttpAsyncClientBuilder httpClientBuilder;

	HttpComponentsClientHttpConnectorBuilder() {
		this(null, new HttpComponentsHttpAsyncClientBuilder());
	}

	private HttpComponentsClientHttpConnectorBuilder(List<Consumer<HttpComponentsClientHttpConnector>> customizers,
			HttpComponentsHttpAsyncClientBuilder httpClientBuilder) {
		super(customizers);
		this.httpClientBuilder = httpClientBuilder;
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpConnectorBuilder} that applies
	 * additional customization to the underlying {@link HttpAsyncClientBuilder}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
	 */
	public HttpComponentsClientHttpConnectorBuilder withHttpClientCustomizer(
			Consumer<HttpAsyncClientBuilder> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'customizer' must not be null");
		return new HttpComponentsClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withCustomizer(httpClientCustomizer));
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpConnectorBuilder} that applies
	 * additional customization to the underlying
	 * {@link PoolingAsyncClientConnectionManagerBuilder}.
	 * @param connectionManagerCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsClientHttpConnectorBuilder} instance
	 */
	public HttpComponentsClientHttpConnectorBuilder withConnectionManagerCustomizer(
			Consumer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer) {
		Assert.notNull(connectionManagerCustomizer, "'connectionManagerCustomizer' must not be null");
		return new HttpComponentsClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withConnectionManagerCustomizer(connectionManagerCustomizer));
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpConnectorBuilder} that applies
	 * additional customization to the underlying
	 * {@link org.apache.hc.client5.http.config.ConnectionConfig.Builder}.
	 * @param connectionConfigCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsClientHttpConnectorBuilder} instance
	 */
	public HttpComponentsClientHttpConnectorBuilder withConnectionConfigCustomizer(
			Consumer<ConnectionConfig.Builder> connectionConfigCustomizer) {
		Assert.notNull(connectionConfigCustomizer, "'connectionConfigCustomizer' must not be null");
		return new HttpComponentsClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withConnectionConfigCustomizer(connectionConfigCustomizer));
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpConnectorBuilder} with a replacement
	 * {@link TlsStrategy} factory.
	 * @param tlsStrategyFactory the new factory used to create a {@link TlsStrategy} for
	 * a given {@link SslBundle}
	 * @return a new {@link HttpComponentsClientHttpConnectorBuilder} instance
	 */
	public HttpComponentsClientHttpConnectorBuilder withTlsSocketStrategyFactory(
			Function<SslBundle, TlsStrategy> tlsStrategyFactory) {
		Assert.notNull(tlsStrategyFactory, "'tlsStrategyFactory' must not be null");
		return new HttpComponentsClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withTlsStrategyFactory(tlsStrategyFactory));
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpConnectorBuilder} that applies
	 * additional customization to the underlying
	 * {@link org.apache.hc.client5.http.config.RequestConfig.Builder} used for default
	 * requests.
	 * @param defaultRequestConfigCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsClientHttpConnectorBuilder} instance
	 */
	public HttpComponentsClientHttpConnectorBuilder withDefaultRequestConfigCustomizer(
			Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer) {
		Assert.notNull(defaultRequestConfigCustomizer, "'defaultRequestConfigCustomizer' must not be null");
		return new HttpComponentsClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withDefaultRequestConfigCustomizer(defaultRequestConfigCustomizer));
	}

	@Override
	protected HttpComponentsClientHttpConnector createClientHttpConnector(ClientHttpConnectorSettings settings) {
		CloseableHttpAsyncClient client = this.httpClientBuilder.build(asHttpClientSettings(settings));
		return new HttpComponentsClientHttpConnector(client);
	}

	static class Classes {

		static final String HTTP_CLIENTS = "org.apache.hc.client5.http.impl.async.HttpAsyncClients";

		static final String REACTIVE_RESPONSE_CONSUMER = "org.apache.hc.core5.reactive.ReactiveResponseConsumer";

		static boolean present(ClassLoader classLoader) {
			return ClassUtils.isPresent(HTTP_CLIENTS, classLoader)
					&& ClassUtils.isPresent(REACTIVE_RESPONSE_CONSUMER, classLoader);
		}

	}

}
