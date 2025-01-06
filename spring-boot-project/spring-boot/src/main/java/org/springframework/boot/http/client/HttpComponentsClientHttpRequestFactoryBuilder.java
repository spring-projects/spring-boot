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

import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.protocol.HttpContext;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
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

	private final Consumer<HttpClientBuilder> httpClientCustomizer;

	private final Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer;

	private final Consumer<SocketConfig.Builder> socketConfigCustomizer;

	private final Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer;

	private final Function<SslBundle, TlsSocketStrategy> tlsSocketStrategyFactory;

	HttpComponentsClientHttpRequestFactoryBuilder() {
		this(Collections.emptyList(), emptyCustomizer(), emptyCustomizer(), emptyCustomizer(), emptyCustomizer(),
				HttpComponentsClientHttpRequestFactoryBuilder::createTlsSocketStrategy);
	}

	private static TlsSocketStrategy createTlsSocketStrategy(SslBundle sslBundle) {
		SslOptions options = sslBundle.getOptions();
		return new DefaultClientTlsStrategy(sslBundle.createSslContext(), options.getEnabledProtocols(),
				options.getCiphers(), null, new DefaultHostnameVerifier());
	}

	private HttpComponentsClientHttpRequestFactoryBuilder(
			List<Consumer<HttpComponentsClientHttpRequestFactory>> customizers,
			Consumer<HttpClientBuilder> httpClientCustomizer,
			Consumer<PoolingHttpClientConnectionManagerBuilder> connectionManagerCustomizer,
			Consumer<SocketConfig.Builder> socketConfigCustomizer,
			Consumer<RequestConfig.Builder> defaultRequestConfigCustomizer,
			Function<SslBundle, TlsSocketStrategy> tlsSocketStrategyFactory) {
		super(customizers);
		this.httpClientCustomizer = httpClientCustomizer;
		this.connectionManagerCustomizer = connectionManagerCustomizer;
		this.socketConfigCustomizer = socketConfigCustomizer;
		this.defaultRequestConfigCustomizer = defaultRequestConfigCustomizer;
		this.tlsSocketStrategyFactory = tlsSocketStrategyFactory;
	}

	@Override
	public HttpComponentsClientHttpRequestFactoryBuilder withCustomizer(
			Consumer<HttpComponentsClientHttpRequestFactory> customizer) {
		return new HttpComponentsClientHttpRequestFactoryBuilder(mergedCustomizers(customizer),
				this.httpClientCustomizer, this.connectionManagerCustomizer, this.socketConfigCustomizer,
				this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
	}

	@Override
	public HttpComponentsClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<HttpComponentsClientHttpRequestFactory>> customizers) {
		return new HttpComponentsClientHttpRequestFactoryBuilder(mergedCustomizers(customizers),
				this.httpClientCustomizer, this.connectionManagerCustomizer, this.socketConfigCustomizer,
				this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
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
				this.httpClientCustomizer.andThen(httpClientCustomizer), this.connectionManagerCustomizer,
				this.socketConfigCustomizer, this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
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
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(), this.httpClientCustomizer,
				this.connectionManagerCustomizer.andThen(connectionManagerCustomizer), this.socketConfigCustomizer,
				this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
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
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(), this.httpClientCustomizer,
				this.connectionManagerCustomizer, this.socketConfigCustomizer.andThen(socketConfigCustomizer),
				this.defaultRequestConfigCustomizer, this.tlsSocketStrategyFactory);
	}

	/**
	 * Return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} with a
	 * replacement {@link TlsSocketStrategy} factory.
	 * @param tlsSocketStrategyFactory the new factory used to create a
	 * {@link TlsSocketStrategy} for a given {@link SslBundle}
	 * @return a new {@link HttpComponentsClientHttpRequestFactoryBuilder} instance
	 */
	public HttpComponentsClientHttpRequestFactoryBuilder withTlsSocketStrategyFactory(
			Function<SslBundle, TlsSocketStrategy> tlsSocketStrategyFactory) {
		Assert.notNull(tlsSocketStrategyFactory, "'tlsSocketStrategyFactory' must not be null");
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(), this.httpClientCustomizer,
				this.connectionManagerCustomizer, this.socketConfigCustomizer, this.defaultRequestConfigCustomizer,
				tlsSocketStrategyFactory);
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
		return new HttpComponentsClientHttpRequestFactoryBuilder(getCustomizers(), this.httpClientCustomizer,
				this.connectionManagerCustomizer, this.socketConfigCustomizer,
				this.defaultRequestConfigCustomizer.andThen(defaultRequestConfigCustomizer),
				this.tlsSocketStrategyFactory);
	}

	@Override
	protected HttpComponentsClientHttpRequestFactory createClientHttpRequestFactory(
			ClientHttpRequestFactorySettings settings) {
		HttpClient httpClient = createHttpClient(settings);
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).asInt(Duration::toMillis).to(factory::setConnectTimeout);
		return factory;
	}

	private HttpClient createHttpClient(ClientHttpRequestFactorySettings settings) {
		HttpClientBuilder builder = HttpClientBuilder.create()
			.useSystemProperties()
			.setRedirectStrategy(asRedirectStrategy(settings.redirects()))
			.setConnectionManager(createConnectionManager(settings))
			.setDefaultRequestConfig(createDefaultRequestConfig());
		this.httpClientCustomizer.accept(builder);
		return builder.build();
	}

	private RedirectStrategy asRedirectStrategy(Redirects redirects) {
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> DefaultRedirectStrategy.INSTANCE;
			case DONT_FOLLOW -> NoFollowRedirectStrategy.INSTANCE;
		};
	}

	private PoolingHttpClientConnectionManager createConnectionManager(ClientHttpRequestFactorySettings settings) {
		PoolingHttpClientConnectionManagerBuilder builder = PoolingHttpClientConnectionManagerBuilder.create()
			.useSystemProperties();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		builder.setDefaultSocketConfig(createSocketConfig(settings));
		map.from(settings::sslBundle).as(this.tlsSocketStrategyFactory).to(builder::setTlsSocketStrategy);
		this.connectionManagerCustomizer.accept(builder);
		return builder.build();
	}

	private SocketConfig createSocketConfig(ClientHttpRequestFactorySettings settings) {
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

	/**
	 * {@link RedirectStrategy} that never follows redirects.
	 */
	private static final class NoFollowRedirectStrategy implements RedirectStrategy {

		private static final RedirectStrategy INSTANCE = new NoFollowRedirectStrategy();

		private NoFollowRedirectStrategy() {
		}

		@Override
		public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
			return false;
		}

		@Override
		public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context) {
			return null;
		}

	}

	static class Classes {

		static final String HTTP_CLIENTS = "org.apache.hc.client5.http.impl.classic.HttpClients";

		static final boolean PRESENT = ClassUtils.isPresent(HTTP_CLIENTS, null);

	}

}
