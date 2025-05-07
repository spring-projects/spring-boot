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
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.net.ssl.SSLException;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider.SslContextSpec;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.util.Assert;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Builder that can be used to create a Rector Netty {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.5.0
 */
public final class ReactorHttpClientBuilder {

	private final Supplier<HttpClient> factory;

	private final UnaryOperator<HttpClient> customizer;

	public ReactorHttpClientBuilder() {
		this(HttpClient::create, UnaryOperator.identity());
	}

	private ReactorHttpClientBuilder(Supplier<HttpClient> httpClientFactory, UnaryOperator<HttpClient> customizer) {
		this.factory = httpClientFactory;
		this.customizer = customizer;
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
				(httpClient) -> this.customizer.apply(httpClient).runOn(reactorResourceFactory.getLoopResources()));
	}

	/**
	 * Return a new {@link ReactorHttpClientBuilder} that uses the given factory to create
	 * the {@link HttpClient}.
	 * @param factory the factory to use
	 * @return a new {@link ReactorHttpClientBuilder} instance
	 */
	public ReactorHttpClientBuilder withHttpClientFactory(Supplier<HttpClient> factory) {
		Assert.notNull(factory, "'factory' must not be null");
		return new ReactorHttpClientBuilder(factory, this.customizer);
	}

	/**
	 * Return a new {@link ReactorHttpClientBuilder} that applies additional customization
	 * to the underlying {@link HttpClient}.
	 * @param customizer the customizer to apply
	 * @return a new {@link ReactorHttpClientBuilder} instance
	 */
	public ReactorHttpClientBuilder withHttpClientCustomizer(UnaryOperator<HttpClient> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return new ReactorHttpClientBuilder(this.factory,
				(httpClient) -> customizer.apply(this.customizer.apply(httpClient)));
	}

	/**
	 * Build a new {@link HttpClient} instance with the given settings applied.
	 * @param settings the settings to apply
	 * @return a new {@link HttpClient} instance
	 */
	public HttpClient build(HttpClientSettings settings) {
		settings = (settings != null) ? settings : HttpClientSettings.DEFAULTS;
		HttpClient httpClient = applyDefaults(this.factory.get());
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		httpClient = map.from(settings::connectTimeout).to(httpClient, this::setConnectTimeout);
		httpClient = map.from(settings::readTimeout).to(httpClient, HttpClient::responseTimeout);
		httpClient = map.from(settings::redirects).as(this::followRedirects).to(httpClient, HttpClient::followRedirect);
		httpClient = map.from(settings::sslBundle).to(httpClient, this::secure);
		return this.customizer.apply(httpClient);
	}

	HttpClient applyDefaults(HttpClient httpClient) {
		// Aligns with Spring Framework defaults
		return httpClient.compress(true);
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

}
