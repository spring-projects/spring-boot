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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.http.client.HttpComponentsHttpClientBuilder.TlsSocketStrategyFactory;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#httpComponents()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.4.0
 */
public final class HttpComponentsClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<HttpComponentsClientHttpRequestFactory> {

	private final HttpComponentsHttpClientBuilder httpClientBuilder;

	HttpComponentsClientHttpRequestFactoryBuilder() {
		this(null, new HttpComponentsHttpClientBuilder());
	}

	private HttpComponentsClientHttpRequestFactoryBuilder(
			@Nullable List<Consumer<HttpComponentsClientHttpRequestFactory>> customizers,
			HttpComponentsHttpClientBuilder httpClientBuilder) {
		super(customizers);
		this.httpClientBuilder = httpClientBuilder;
	}

	@Override
	public HttpComponentsClientHttpRequestFactoryBuilder withCustomizer(
			Consumer<HttpComponentsClientHttpRequestFactory> customizer) {
		return new HttpComponentsClientHttpRequestFactoryBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
	}

	@Override
	public HttpComponentsClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<HttpComponentsClientHttpRequestFactory>> customizers) {
		return new HttpComponentsClientHttpRequestFactoryBuilder(mergedCustomizers(customizers),
				this.httpClientBuilder);
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies
	 * additional customization to the underlying {@link HttpClientBuilder}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
	 */
	public HttpComponentsClientHttpRequestFactoryBuilder withHttpClientCustomizer(
			Consumer<HttpClientBuilder> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' must not be null");
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withCustomizer(httpClientCustomizer));
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies
	 * additional customization to the underlying
	 * {@link PoolingHttpClientConnectionManagerBuilder}.
	 * @param connectionManagerCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
	 */
	public HttpComponentsClientHttpRequestFactoryBuilder withConnectionManagerCustomizer(
			Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer) {
		Assert.notNull(connectionManagerCustomizer, "'connectionManagerCustomizer' must not be null");
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withConnectionManagerCustomizer(connectionManagerCustomizer));
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies
	 * additional customization to the underlying
	 * {@link org.apache.hc.core5.http.io.SocketConfig.Builder}.
	 * @param socketConfigCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
	 */
	public HttpComponentsClientHttpRequestFactoryBuilder withSocketConfigCustomizer(
			Consumer<SocketConfig.Builder> socketConfigCustomizer) {
		Assert.notNull(socketConfigCustomizer, "'socketConfigCustomizer' must not be null");
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withSocketConfigCustomizer(socketConfigCustomizer));
	}

	/**
	 * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
	 * customization to the underlying
	 * {@link org.apache.hc.client5.http.config.ConnectionConfig.Builder}.
	 * @param connectionConfigCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpClientBuilder} instance
	 */
	public HttpComponentsClientHttpRequestFactoryBuilder withConnectionConfigCustomizer(
			Consumer<ConnectionConfig.Builder> connectionConfigCustomizer) {
		Assert.notNull(connectionConfigCustomizer, "'connectionConfigCustomizer' must not be null");
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withConnectionConfigCustomizer(connectionConfigCustomizer));
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} with a
	 * replacement {@link TlsSocketStrategy} factory.
	 * @param tlsSocketStrategyFactory the new factory used to create a
	 * {@link TlsSocketStrategy} for a given {@link SslBundle}
	 * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
	 */
	public HttpComponentsClientHttpRequestFactoryBuilder withTlsSocketStrategyFactory(
			TlsSocketStrategyFactory tlsSocketStrategyFactory) {
		Assert.notNull(tlsSocketStrategyFactory, "'tlsSocketStrategyFactory' must not be null");
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withTlsSocketStrategyFactory(tlsSocketStrategyFactory));
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies
	 * additional customization to the underlying
	 * {@link org.apache.hc.client5.http.config.RequestConfig.Builder} used for default
	 * requests.
	 * @param defaultRequestConfigCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
	 */
	public HttpComponentsClientHttpRequestFactoryBuilder withDefaultRequestConfigCustomizer(
			Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer) {
		Assert.notNull(defaultRequestConfigCustomizer, "'defaultRequestConfigCustomizer' must not be null");
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withDefaultRequestConfigCustomizer(defaultRequestConfigCustomizer));
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} that applies the
	 * given customizer. This can be useful for applying pre-packaged customizations.
	 * @param customizer the customizer to apply
	 * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder}
	 * @since 4.0.0
	 */
	public HttpComponentsClientHttpRequestFactoryBuilder with(
			UnaryOperator<HttpComponentsClientHttpRequestFactoryBuilder> customizer) {
		return customizer.apply(this);
	}

	@Override
	protected HttpComponentsClientHttpRequestFactory createClientHttpRequestFactory(HttpClientSettings settings) {
		HttpClient httpClient = this.httpClientBuilder.build(settings);
		return new HttpComponentsClientHttpRequestFactory(httpClient);
	}

	static class Classes {

		static final String HTTP_CLIENTS = "org.apache.hc.client5.http.impl.classic.HttpClients";

		static boolean present(@Nullable ClassLoader classLoader) {
			return ClassUtils.isPresent(HTTP_CLIENTS, classLoader);
		}

	}

}
