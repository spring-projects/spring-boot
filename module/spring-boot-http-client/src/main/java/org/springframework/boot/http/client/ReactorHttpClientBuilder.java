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

import java.time.Duration;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.net.ssl.SSLException;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import org.jspecify.annotations.Nullable;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientConfig;
import reactor.netty.tcp.SslProvider.SslContextSpec;
import reactor.netty.transport.ClientTransport.ResolvedAddressSelector;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Builder that can be used to create a Reactor Netty {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.5.0
 */
public final class ReactorHttpClientBuilder {

	private final Supplier<HttpClient> factory;

	private final UnaryOperator<HttpClient> factoryDefaults;

	private final UnaryOperator<HttpClient> customizer;

	private final @Nullable ResolvedAddressSelector<? super HttpClientConfig> resolvedAddressSelector;

	public ReactorHttpClientBuilder() {
		this(HttpClient::create, ReactorHttpClientBuilder::applySpringDefaults, UnaryOperator.identity(), null);
	}

	private ReactorHttpClientBuilder(Supplier<HttpClient> factory, UnaryOperator<HttpClient> factoryDefaults,
			UnaryOperator<HttpClient> customizer,
			@Nullable ResolvedAddressSelector<? super HttpClientConfig> resolvedAddressSelector) {
		this.factory = factory;
		this.factoryDefaults = factoryDefaults;
		this.customizer = customizer;
		this.resolvedAddressSelector = resolvedAddressSelector;
	}

	/**
	 * Return a new {@link ReactorHttpClientBuilder} that uses the given
	 * {@link ReactorResourceFactory} to create the {@link HttpClient}.
	 * @param reactorResourceFactory the {@link ReactorResourceFactory} to use
	 * @return a new {@link ReactorHttpClientBuilder} instance
	 */
	public ReactorHttpClientBuilder withReactorResourceFactory(ReactorResourceFactory reactorResourceFactory) {
		Assert.notNull(reactorResourceFactory, "'reactorResourceFactory' must not be null");
		return new ReactorHttpClientBuilder(() -> HttpClient.create(reactorResourceFactory.getConnectionProvider()),
				this.factoryDefaults,
				(httpClient) -> this.customizer.apply(httpClient).runOn(reactorResourceFactory.getLoopResources()),
				this.resolvedAddressSelector);
	}

	/**
	 * Return a new {@link ReactorHttpClientBuilder} that uses the given factory to create
	 * the {@link HttpClient}.
	 * @param factory the factory to use
	 * @return a new {@link ReactorHttpClientBuilder} instance
	 */
	public ReactorHttpClientBuilder withHttpClientFactory(Supplier<HttpClient> factory) {
		Assert.notNull(factory, "'factory' must not be null");
		return new ReactorHttpClientBuilder(factory, this.factoryDefaults, this.customizer,
				this.resolvedAddressSelector);
	}

	/**
	 * Return a new {@link ReactorHttpClientBuilder} that does not apply any defaults when
	 * first creating the {@link HttpClient}.
	 * @return a new {@link ReactorHttpClientBuilder} instance
	 * @since 4.1.0
	 */
	public ReactorHttpClientBuilder withoutHttpClientDefaults() {
		return withHttpClientDefaults(null);
	}

	/**
	 * Return a new {@link ReactorHttpClientBuilder} that applies the given factory
	 * defaults when first creating the {@link HttpClient}.
	 * @param factoryDefaults the factory to use
	 * @return a new {@link ReactorHttpClientBuilder} instance
	 * @since 4.1.0
	 */
	public ReactorHttpClientBuilder withHttpClientDefaults(@Nullable UnaryOperator<HttpClient> factoryDefaults) {
		return new ReactorHttpClientBuilder(this.factory,
				(factoryDefaults != null) ? factoryDefaults : UnaryOperator.identity(), this.customizer,
				this.resolvedAddressSelector);
	}

	/**
	 * Return a new {@link ReactorHttpClientBuilder} that applies additional customization
	 * to the underlying {@link HttpClient}.
	 * @param customizer the customizer to apply
	 * @return a new {@link ReactorHttpClientBuilder} instance
	 */
	public ReactorHttpClientBuilder withHttpClientCustomizer(UnaryOperator<HttpClient> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return new ReactorHttpClientBuilder(this.factory, this.factoryDefaults,
				(httpClient) -> customizer.apply(this.customizer.apply(httpClient)), this.resolvedAddressSelector);
	}

	/**
	 * Return a new {@link ReactorHttpClientBuilder} that uses the
	 * {@link ResolvedAddressSelector}. This method should be used in favor of a
	 * customizer so that {@link HttpClientSettings#inetAddressFilter()} can be applied.
	 * @param resolvedAddressSelector the resolved address selector to use
	 * @return a new {@link ReactorHttpClientBuilder} instance
	 * @since 4.1.0
	 */
	public ReactorHttpClientBuilder withResolvedAddressSelector(
			ResolvedAddressSelector<? super HttpClientConfig> resolvedAddressSelector) {
		Assert.notNull(resolvedAddressSelector, "'resolvedAddressSelector' must not be null");
		return new ReactorHttpClientBuilder(this.factory, this.factoryDefaults, this.customizer,
				resolvedAddressSelector);
	}

	/**
	 * Build a new {@link HttpClient} instance with the given settings applied.
	 * @param settings the settings to apply
	 * @return a new {@link HttpClient} instance
	 */
	public HttpClient build(@Nullable HttpClientSettings settings) {
		settings = (settings != null) ? settings : HttpClientSettings.defaults();
		HttpClient httpClient = this.factoryDefaults.apply(this.factory.get());
		PropertyMapper map = PropertyMapper.get();
		httpClient = map.from(settings::connectTimeout).to(httpClient, this::setConnectTimeout);
		httpClient = map.from(settings::readTimeout).to(httpClient, HttpClient::responseTimeout);
		httpClient = map.from(settings::redirects)
			.orFrom(() -> HttpRedirects.FOLLOW_WHEN_POSSIBLE)
			.as(this::followRedirects)
			.to(httpClient, HttpClient::followRedirect);
		if (HttpCookieHandling.ENABLE.equals(settings.cookieHandling())) {
			throw new IllegalArgumentException("Reactor Netty HTTP client does not support cookie handling");
		}
		httpClient = map.from(settings::sslBundle).to(httpClient, this::secure);
		httpClient = map.from(resolvedAddressSelector(settings.inetAddressFilter()))
			.to(httpClient, HttpClient::resolvedAddressesSelector);
		return this.customizer.apply(httpClient);
	}

	private @Nullable ResolvedAddressSelector<? super HttpClientConfig> resolvedAddressSelector(
			@Nullable InetAddressFilter inetAddressFilter) {
		return (inetAddressFilter != null)
				? new ReactorFilteredResolvedAddressSelector<>(this.resolvedAddressSelector, inetAddressFilter)
				: this.resolvedAddressSelector;
	}

	private HttpClient setConnectTimeout(HttpClient httpClient, Duration timeout) {
		return httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) timeout.toMillis());
	}

	private boolean followRedirects(HttpRedirects redirects) {
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> true;
			case DONT_FOLLOW -> false;
		};
	}

	private HttpClient secure(HttpClient httpClient, SslBundle sslBundle) {
		return httpClient.secure((ThrowingConsumer.of((spec) -> configureSsl(spec, sslBundle))));
	}

	private void configureSsl(SslContextSpec spec, SslBundle sslBundle) throws SSLException {
		SslOptions options = sslBundle.getOptions();
		SslManagerBundle managers = sslBundle.getManagers();
		SslContextBuilder builder = SslContextBuilder.forClient()
			.keyManager(managers.getKeyManagerFactory())
			.trustManager(managers.getTrustManagerFactory())
			.ciphers(SslOptions.asSet(options.getCiphers()))
			.protocols(options.getEnabledProtocols());
		spec.sslContext(builder.build());
	}

	static HttpClient applySpringDefaults(HttpClient httpClient) {
		// Aligns with Spring Framework defaults in ReactorClientHttpRequestFactory
		return httpClient.compress(true).proxyWithSystemProperties();
	}

}
