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

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.io.SocketConfig;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;

/**
 * Builder that can be used to create a
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.5.0
 */
public final class HttpComponentsHttpClientBuilder {

	private final Consumer<HttpClientBuilder> customizer;

	private final Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer;

	private final Consumer<SocketConfig.Builder> socketConfigCustomizer;

	private final Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer;

	private final Function<SslBundle, TlsSocketStrategy> tlsSocketStrategyFactory;

	public HttpComponentsHttpClientBuilder() {
		this(Empty.consumer(), Empty.consumer(), Empty.consumer(), Empty.consumer(),
				HttpComponentsSslBundleTlsStrategy::get);
	}

	private HttpComponentsHttpClientBuilder(Consumer<HttpClientBuilder> customizer,
			Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer,
			Consumer<SocketConfig.Builder> socketConfigCustomizer,
			Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer,
			Function<SslBundle, TlsSocketStrategy> tlsSocketStrategyFactory) {
		this.customizer = customizer;
		this.connectionManagerCustomizer = connectionManagerCustomizer;
		this.socketConfigCustomizer = socketConfigCustomizer;
		this.defaultRequestConfigCustomizer = defaultRequestConfigCustomizer;
		this.tlsSocketStrategyFactory = tlsSocketStrategyFactory;
	}

	/**
	 * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
	 * customization to the underlying {@link HttpClientBuilder}.
	 * @param customizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpClientBuilder} instance
	 */
	public HttpComponentsHttpClientBuilder withCustomizer(Consumer<HttpClientBuilder> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return new HttpComponentsHttpClientBuilder(this.customizer.andThen(customizer),
				this.connectionManagerCustomizer, this.socketConfigCustomizer, this.defaultRequestConfigCustomizer,
				this.tlsSocketStrategyFactory);
	}

	/**
	 * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
	 * customization to the underlying {@link PoolingHttpClientConnectionManagerBuilder}.
	 * @param connectionManagerCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpClientBuilder} instance
	 */
	public HttpComponentsHttpClientBuilder withConnectionManagerCustomizer(
			Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer) {
		Assert.notNull(connectionManagerCustomizer, "'connectionManagerCustomizer' must not be null");
		return new HttpComponentsHttpClientBuilder(this.customizer,
				this.connectionManagerCustomizer.andThen(connectionManagerCustomizer), this.socketConfigCustomizer,
				this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
	}

	/**
	 * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
	 * customization to the underlying
	 * {@link org.apache.hc.core5.http.io.SocketConfig.Builder}.
	 * @param socketConfigCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpClientBuilder} instance
	 */
	public HttpComponentsHttpClientBuilder withSocketConfigCustomizer(
			Consumer<SocketConfig.Builder> socketConfigCustomizer) {
		Assert.notNull(socketConfigCustomizer, "'socketConfigCustomizer' must not be null");
		return new HttpComponentsHttpClientBuilder(this.customizer, this.connectionManagerCustomizer,
				this.socketConfigCustomizer.andThen(socketConfigCustomizer), this.defaultRequestConfigCustomizer,
				this.tlsSocketStrategyFactory);
	}

	/**
	 * Return a new {@link HttpComponentsHttpClientBuilder} with a replacement
	 * {@link TlsSocketStrategy} factory.
	 * @param tlsSocketStrategyFactory the new factory used to create a
	 * {@link TlsSocketStrategy}. The function will be provided with a {@link SslBundle}
	 * or {@code null} if no bundle is selected. Only non {@code null} results will be
	 * applied.
	 * @return a new {@link HttpComponentsHttpClientBuilder} instance
	 */
	public HttpComponentsHttpClientBuilder withTlsSocketStrategyFactory(
			Function<SslBundle, TlsSocketStrategy> tlsSocketStrategyFactory) {
		Assert.notNull(tlsSocketStrategyFactory, "'tlsSocketStrategyFactory' must not be null");
		return new HttpComponentsHttpClientBuilder(this.customizer, this.connectionManagerCustomizer,
				this.socketConfigCustomizer, this.defaultRequestConfigCustomizer, tlsSocketStrategyFactory);
	}

	/**
	 * Return a new {@link HttpComponentsHttpClientBuilder} that applies additional
	 * customization to the underlying
	 * {@link org.apache.hc.client5.http.config.RequestConfig.Builder} used for default
	 * requests.
	 * @param defaultRequestConfigCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpClientBuilder} instance
	 */
	public HttpComponentsHttpClientBuilder withDefaultRequestConfigCustomizer(
			Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer) {
		Assert.notNull(defaultRequestConfigCustomizer, "'defaultRequestConfigCustomizer' must not be null");
		return new HttpComponentsHttpClientBuilder(this.customizer, this.connectionManagerCustomizer,
				this.socketConfigCustomizer,
				this.defaultRequestConfigCustomizer.andThen(defaultRequestConfigCustomizer),
				this.tlsSocketStrategyFactory);
	}

	/**
	 * Build a new {@link HttpClient} instance with the given settings applied.
	 * @param settings the settings to apply
	 * @return a new {@link HttpClient} instance
	 */
	public CloseableHttpClient build(HttpClientSettings settings) {
		settings = (settings != null) ? settings : HttpClientSettings.DEFAULTS;
		Assert.isTrue(settings.connectTimeout() == null, "'settings' must not have a 'connectTimeout'");
		HttpClientBuilder builder = HttpClientBuilder.create()
			.useSystemProperties()
			.setRedirectStrategy(HttpComponentsRedirectStrategy.get(settings.redirects()))
			.setConnectionManager(createConnectionManager(settings))
			.setDefaultRequestConfig(createDefaultRequestConfig());
		this.customizer.accept(builder);
		return builder.build();
	}

	private PoolingHttpClientConnectionManager createConnectionManager(HttpClientSettings settings) {
		PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create()
			.useSystemProperties();
		PropertyMapper map = PropertyMapper.get();
		builder.setDefaultSocketConfig(createSocketConfig(settings));
		map.from(settings::sslBundle).as(this.tlsSocketStrategyFactory).whenNonNull().to(builder::setTlsSocketStrategy);
		this.connectionManagerCustomizer.accept(builder);
		return builder.build();
	}

	private SocketConfig createSocketConfig(HttpClientSettings settings) {
		SocketConfig.Builder builder = SocketConfig.custom();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::readTimeout)
			.asInt(Duration::toMillis)
			.to((timeout) -> builder.setSoTimeout(timeout, TimeUnit.MILLISECONDS));
		this.socketConfigCustomizer.accept(builder);
		return builder.build();
	}

	private RequestConfig createDefaultRequestConfig() {
		RequestConfig.Builder builder = RequestConfig.custom();
		this.defaultRequestConfigCustomizer.accept(builder);
		return builder.build();
	}

}
