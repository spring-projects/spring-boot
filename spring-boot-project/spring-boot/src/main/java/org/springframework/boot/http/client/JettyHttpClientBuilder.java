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

import javax.net.ssl.SSLContext;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.util.Assert;

/**
 * Builder that can be used to create a Jetty {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.5.0
 */
public final class JettyHttpClientBuilder {

	private final Consumer<HttpClient> customizer;

	private final Consumer<HttpClientTransport> httpClientTransportCustomizer;

	private final Consumer<ClientConnector> clientConnectorCustomizerCustomizer;

	public JettyHttpClientBuilder() {
		this(Empty.consumer(), Empty.consumer(), Empty.consumer());
	}

	private JettyHttpClientBuilder(Consumer<HttpClient> customizer,
			Consumer<HttpClientTransport> httpClientTransportCustomizer,
			Consumer<ClientConnector> clientConnectorCustomizerCustomizer) {
		this.customizer = customizer;
		this.httpClientTransportCustomizer = httpClientTransportCustomizer;
		this.clientConnectorCustomizerCustomizer = clientConnectorCustomizerCustomizer;
	}

	/**
	 * Return a new {@link JettyClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link HttpClient}.
	 * @param customizer the customizer to apply
	 * @return a new {@link JettyClientHttpRequestFactoryBuilder} instance
	 */
	public JettyHttpClientBuilder withCustomizer(Consumer<HttpClient> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return new JettyHttpClientBuilder(this.customizer.andThen(customizer), this.httpClientTransportCustomizer,
				this.clientConnectorCustomizerCustomizer);
	}

	/**
	 * Return a new {@link JettyClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link HttpClientTransport}.
	 * @param httpClientTransportCustomizer the customizer to apply
	 * @return a new {@link JettyClientHttpRequestFactoryBuilder} instance
	 */
	public JettyHttpClientBuilder withHttpClientTransportCustomizer(
			Consumer<HttpClientTransport> httpClientTransportCustomizer) {
		Assert.notNull(httpClientTransportCustomizer, "'httpClientTransportCustomizer' must not be null");
		return new JettyHttpClientBuilder(this.customizer,
				this.httpClientTransportCustomizer.andThen(httpClientTransportCustomizer),
				this.clientConnectorCustomizerCustomizer);
	}

	/**
	 * Return a new {@link JettyClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link ClientConnector}.
	 * @param clientConnectorCustomizerCustomizer the customizer to apply
	 * @return a new {@link JettyClientHttpRequestFactoryBuilder} instance
	 */
	public JettyHttpClientBuilder withClientConnectorCustomizerCustomizer(
			Consumer<ClientConnector> clientConnectorCustomizerCustomizer) {
		Assert.notNull(clientConnectorCustomizerCustomizer, "'clientConnectorCustomizerCustomizer' must not be null");
		return new JettyHttpClientBuilder(this.customizer, this.httpClientTransportCustomizer,
				this.clientConnectorCustomizerCustomizer.andThen(clientConnectorCustomizerCustomizer));
	}

	/**
	 * Build a new {@link HttpClient} instance with the given settings applied.
	 * @param settings the settings to apply
	 * @return a new {@link HttpClient} instance
	 */
	public HttpClient build(HttpClientSettings settings) {
		settings = (settings != null) ? settings : HttpClientSettings.DEFAULTS;
		HttpClientTransport transport = createTransport(settings);
		this.httpClientTransportCustomizer.accept(transport);
		HttpClient httpClient = createHttpClient(settings.readTimeout(), transport);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).as(Duration::toMillis).to(httpClient::setConnectTimeout);
		map.from(settings::redirects).as(this::followRedirects).to(httpClient::setFollowRedirects);
		this.customizer.accept(httpClient);
		return httpClient;
	}

	private HttpClient createHttpClient(Duration readTimeout, HttpClientTransport transport) {
		return (readTimeout != null) ? new HttpClientWithReadTimeout(transport, readTimeout)
				: new HttpClient(transport);
	}

	private HttpClientTransport createTransport(HttpClientSettings settings) {
		ClientConnector connector = createClientConnector(settings.sslBundle());
		return (connector.getSslContextFactory() != null) ? new HttpClientTransportDynamic(connector)
				: new HttpClientTransportOverHTTP(connector);
	}

	private ClientConnector createClientConnector(SslBundle sslBundle) {
		ClientConnector connector = new ClientConnector();
		if (sslBundle != null) {
			connector.setSslContextFactory(createSslContextFactory(sslBundle));
		}
		this.clientConnectorCustomizerCustomizer.accept(connector);
		return connector;
	}

	private SslContextFactory.Client createSslContextFactory(SslBundle sslBundle) {
		SslOptions options = sslBundle.getOptions();
		SSLContext sslContext = sslBundle.createSslContext();
		SslContextFactory.Client factory = new SslContextFactory.Client();
		factory.setSslContext(sslContext);
		if (options.getCiphers() != null) {
			factory.setIncludeCipherSuites(options.getCiphers());
			factory.setExcludeCipherSuites();
		}
		if (options.getEnabledProtocols() != null) {
			factory.setIncludeProtocols(options.getEnabledProtocols());
			factory.setExcludeProtocols();
		}
		return factory;
	}

	private boolean followRedirects(HttpRedirects redirects) {
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> true;
			case DONT_FOLLOW -> false;
		};
	}

	/**
	 * {@link HttpClient} subclass that sets the read timeout.
	 */
	static class HttpClientWithReadTimeout extends HttpClient {

		private final Duration readTimeout;

		HttpClientWithReadTimeout(HttpClientTransport transport, Duration readTimeout) {
			super(transport);
			this.readTimeout = readTimeout;
		}

		@Override
		public org.eclipse.jetty.client.Request newRequest(java.net.URI uri) {
			Request request = super.newRequest(uri);
			request.timeout(this.readTimeout.toMillis(), TimeUnit.MILLISECONDS);
			return request;
		}

	}

}
