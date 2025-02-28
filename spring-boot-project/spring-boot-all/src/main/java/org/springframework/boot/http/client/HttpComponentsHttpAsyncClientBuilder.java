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

package org.springframework.boot.http.client;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;

/**
 * Builder that can be used to create a
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * {@link HttpAsyncClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.5.0
 */
public final class HttpComponentsHttpAsyncClientBuilder {

	private final Consumer<HttpAsyncClientBuilder> customizer;

	private final Consumer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer;

	private final Consumer<ConnectionConfig.Builder> connectionConfigCustomizer;

	private final Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer;

	private final Function<SslBundle, TlsStrategy> tlsStrategyFactory;

	public HttpComponentsHttpAsyncClientBuilder() {
		this(Empty.consumer(), Empty.consumer(), Empty.consumer(), Empty.consumer(),
				HttpComponentsSslBundleTlsStrategy::get);
	}

	private HttpComponentsHttpAsyncClientBuilder(Consumer<HttpAsyncClientBuilder> customizer,
			Consumer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer,
			Consumer<ConnectionConfig.Builder> connectionConfigCustomizer,
			Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer,
			Function<SslBundle, TlsStrategy> tlsStrategyFactory) {
		this.customizer = customizer;
		this.connectionManagerCustomizer = connectionManagerCustomizer;
		this.connectionConfigCustomizer = connectionConfigCustomizer;
		this.defaultRequestConfigCustomizer = defaultRequestConfigCustomizer;
		this.tlsStrategyFactory = tlsStrategyFactory;
	}

	/**
	 * Return a new {@link HttpComponentsHttpAsyncClientBuilder} that applies additional
	 * customization to the underlying {@link HttpAsyncClientBuilder}.
	 * @param customizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
	 */
	public HttpComponentsHttpAsyncClientBuilder withCustomizer(Consumer<HttpAsyncClientBuilder> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return new HttpComponentsHttpAsyncClientBuilder(this.customizer.andThen(customizer),
				this.connectionManagerCustomizer, this.connectionConfigCustomizer, this.defaultRequestConfigCustomizer,
				this.tlsStrategyFactory);
	}

	/**
	 * Return a new {@link HttpComponentsHttpAsyncClientBuilder} that applies additional
	 * customization to the underlying {@link PoolingAsyncClientConnectionManagerBuilder}.
	 * @param connectionManagerCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
	 */
	public HttpComponentsHttpAsyncClientBuilder withConnectionManagerCustomizer(
			Consumer<PoolingAsyncClientConnectionManagerBuilder> connectionManagerCustomizer) {
		Assert.notNull(connectionManagerCustomizer, "'connectionManagerCustomizer' must not be null");
		return new HttpComponentsHttpAsyncClientBuilder(this.customizer,
				this.connectionManagerCustomizer.andThen(connectionManagerCustomizer), this.connectionConfigCustomizer,
				this.defaultRequestConfigCustomizer, this.tlsStrategyFactory);
	}

	/**
	 * Return a new {@link HttpComponentsHttpAsyncClientBuilder} that applies additional
	 * customization to the underlying
	 * {@link org.apache.hc.client5.http.config.ConnectionConfig.Builder}.
	 * @param connectionConfigCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
	 */
	public HttpComponentsHttpAsyncClientBuilder withConnectionConfigCustomizer(
			Consumer<ConnectionConfig.Builder> connectionConfigCustomizer) {
		Assert.notNull(connectionConfigCustomizer, "'connectionConfigCustomizer' must not be null");
		return new HttpComponentsHttpAsyncClientBuilder(this.customizer, this.connectionManagerCustomizer,
				this.connectionConfigCustomizer.andThen(connectionConfigCustomizer),
				this.defaultRequestConfigCustomizer, this.tlsStrategyFactory);
	}

	/**
	 * Return a new {@link HttpComponentsHttpAsyncClientBuilder} with a replacement
	 * {@link TlsStrategy} factory.
	 * @param tlsStrategyFactory the new factory used to create a {@link TlsStrategy} for
	 * a given {@link SslBundle}
	 * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
	 */
	public HttpComponentsHttpAsyncClientBuilder withTlsStrategyFactory(
			Function<SslBundle, TlsStrategy> tlsStrategyFactory) {
		Assert.notNull(tlsStrategyFactory, "'tlsStrategyFactory' must not be null");
		return new HttpComponentsHttpAsyncClientBuilder(this.customizer, this.connectionManagerCustomizer,
				this.connectionConfigCustomizer, this.defaultRequestConfigCustomizer, tlsStrategyFactory);
	}

	/**
	 * Return a new {@link HttpComponentsHttpAsyncClientBuilder} that applies additional
	 * customization to the underlying
	 * {@link org.apache.hc.client5.http.config.RequestConfig.Builder} used for default
	 * requests.
	 * @param defaultRequestConfigCustomizer the customizer to apply
	 * @return a new {@link HttpComponentsHttpAsyncClientBuilder} instance
	 */
	public HttpComponentsHttpAsyncClientBuilder withDefaultRequestConfigCustomizer(
			Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer) {
		Assert.notNull(defaultRequestConfigCustomizer, "'defaultRequestConfigCustomizer' must not be null");
		return new HttpComponentsHttpAsyncClientBuilder(this.customizer, this.connectionManagerCustomizer,
				this.connectionConfigCustomizer,
				this.defaultRequestConfigCustomizer.andThen(defaultRequestConfigCustomizer), this.tlsStrategyFactory);
	}

	/**
	 * Build a new {@link HttpAsyncClient} instance with the given settings applied.
	 * @param settings the settings to apply
	 * @return a new {@link CloseableHttpAsyncClient} instance
	 */
	public CloseableHttpAsyncClient build(HttpClientSettings settings) {
		settings = (settings != null) ? settings : HttpClientSettings.DEFAULTS;
		HttpAsyncClientBuilder builder = HttpAsyncClientBuilder.create()
			.useSystemProperties()
			.setRedirectStrategy(HttpComponentsRedirectStrategy.get(settings.redirects()))
			.setConnectionManager(createConnectionManager(settings))
			.setDefaultRequestConfig(createDefaultRequestConfig());
		this.customizer.accept(builder);
		return builder.build();
	}

	private PoolingAsyncClientConnectionManager createConnectionManager(HttpClientSettings settings) {
		PoolingAsyncClientConnectionManagerBuilder builder = PoolingAsyncClientConnectionManagerBuilder.create()
			.useSystemProperties();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		builder.setDefaultConnectionConfig(createConnectionConfig(settings));
		map.from(settings::sslBundle).as(this.tlsStrategyFactory).to(builder::setTlsStrategy);
		this.connectionManagerCustomizer.accept(builder);
		return builder.build();
	}

	private ConnectionConfig createConnectionConfig(HttpClientSettings settings) {
		ConnectionConfig.Builder builder = ConnectionConfig.custom();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout)
			.as(Duration::toMillis)
			.to((timeout) -> builder.setConnectTimeout(timeout, TimeUnit.MILLISECONDS));
		map.from(settings::readTimeout)
			.asInt(Duration::toMillis)
			.to((timeout) -> builder.setSocketTimeout(timeout, TimeUnit.MILLISECONDS));
		this.connectionConfigCustomizer.accept(builder);
		return builder.build();
	}

	private RequestConfig createDefaultRequestConfig() {
		RequestConfig.Builder builder = RequestConfig.custom();
		this.defaultRequestConfigCustomizer.accept(builder);
		return builder.build();
	}

}
