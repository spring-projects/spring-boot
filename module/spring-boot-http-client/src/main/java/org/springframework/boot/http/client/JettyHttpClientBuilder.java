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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.SSLContext;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.http.HttpCookieStore;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jspecify.annotations.Nullable;

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

	private final Function<ClientConnector, HttpClientTransport> httpClientTransportFactory;

	private final Consumer<HttpClientTransport> httpClientTransportCustomizer;

	private final Consumer<ClientConnector> clientConnectorCustomizerCustomizer;

	private final @Nullable SocketAddressResolver socketAddressResolver;

	public JettyHttpClientBuilder() {
		this(Empty.consumer(), JettyHttpClientBuilder::createHttpClientTransport, Empty.consumer(), Empty.consumer(),
				null);
	}

	private JettyHttpClientBuilder(Consumer<HttpClient> customizer,
			Function<ClientConnector, HttpClientTransport> httpClientTransportFactory,
			Consumer<HttpClientTransport> httpClientTransportCustomizer,
			Consumer<ClientConnector> clientConnectorCustomizerCustomizer,
			@Nullable SocketAddressResolver socketAddressResolver) {
		this.customizer = customizer;
		this.httpClientTransportFactory = httpClientTransportFactory;
		this.httpClientTransportCustomizer = httpClientTransportCustomizer;
		this.clientConnectorCustomizerCustomizer = clientConnectorCustomizerCustomizer;
		this.socketAddressResolver = socketAddressResolver;
	}

	private static HttpClientTransport createHttpClientTransport(ClientConnector connector) {
		return (connector.getSslContextFactory() != null) ? new HttpClientTransportDynamic(connector)
				: new HttpClientTransportOverHTTP(connector);
	}

	/**
	 * Return a new {@link JettyHttpClientBuilder} that applies additional customization
	 * to the underlying {@link HttpClient}.
	 * @param customizer the customizer to apply
	 * @return a new {@link JettyHttpClientBuilder} instance
	 */
	public JettyHttpClientBuilder withCustomizer(Consumer<HttpClient> customizer) {
		Assert.notNull(customizer, "'customizer' must not be null");
		return new JettyHttpClientBuilder(this.customizer.andThen(customizer), this.httpClientTransportFactory,
				this.httpClientTransportCustomizer, this.clientConnectorCustomizerCustomizer,
				this.socketAddressResolver);
	}

	/**
	 * Return a new {@link JettyHttpClientBuilder} that uses the given factory to create
	 * the {@link HttpClientTransport}.
	 * @param httpClientTransportFactory the {@link HttpClientTransport} factory to use
	 * @return a new {@link JettyHttpClientBuilder} instance
	 * @since 4.0.0
	 */
	public JettyHttpClientBuilder withHttpClientTransportFactory(
			Function<ClientConnector, HttpClientTransport> httpClientTransportFactory) {
		Assert.notNull(httpClientTransportFactory, "'httpClientTransportFactory' must not be null");
		return new JettyHttpClientBuilder(this.customizer, httpClientTransportFactory,
				this.httpClientTransportCustomizer, this.clientConnectorCustomizerCustomizer,
				this.socketAddressResolver);
	}

	/**
	 * Return a new {@link JettyHttpClientBuilder} that applies additional customization
	 * to the underlying {@link HttpClientTransport}.
	 * @param httpClientTransportCustomizer the customizer to apply
	 * @return a new {@link JettyHttpClientBuilder} instance
	 */
	public JettyHttpClientBuilder withHttpClientTransportCustomizer(
			Consumer<HttpClientTransport> httpClientTransportCustomizer) {
		Assert.notNull(httpClientTransportCustomizer, "'httpClientTransportCustomizer' must not be null");
		return new JettyHttpClientBuilder(this.customizer, this.httpClientTransportFactory,
				this.httpClientTransportCustomizer.andThen(httpClientTransportCustomizer),
				this.clientConnectorCustomizerCustomizer, this.socketAddressResolver);
	}

	/**
	 * Return a new {@link JettyHttpClientBuilder} that applies additional customization
	 * to the underlying {@link ClientConnector}.
	 * @param clientConnectorCustomizerCustomizer the customizer to apply
	 * @return a new {@link JettyHttpClientBuilder} instance
	 */
	public JettyHttpClientBuilder withClientConnectorCustomizerCustomizer(
			Consumer<ClientConnector> clientConnectorCustomizerCustomizer) {
		Assert.notNull(clientConnectorCustomizerCustomizer, "'clientConnectorCustomizerCustomizer' must not be null");
		return new JettyHttpClientBuilder(this.customizer, this.httpClientTransportFactory,
				this.httpClientTransportCustomizer,
				this.clientConnectorCustomizerCustomizer.andThen(clientConnectorCustomizerCustomizer),
				this.socketAddressResolver);
	}

	/**
	 * Return a new {@link JettyHttpClientBuilder} with a replacement
	 * {@link SocketAddressResolver}.
	 * @param socketAddressResolver the new socket address resolver
	 * @return a new {@link JettyHttpClientBuilder} instance
	 * @since 4.1.0
	 */
	public JettyHttpClientBuilder withSocketAddressResolver(SocketAddressResolver socketAddressResolver) {
		Assert.notNull(socketAddressResolver, "'socketAddressResolver' must not be null");
		return new JettyHttpClientBuilder(this.customizer, this.httpClientTransportFactory,
				this.httpClientTransportCustomizer, this.clientConnectorCustomizerCustomizer, socketAddressResolver);
	}

	/**
	 * Build a new {@link HttpClient} instance with the given settings applied.
	 * @param settings the settings to apply
	 * @return a new {@link HttpClient} instance
	 */
	public HttpClient build(@Nullable HttpClientSettings settings) {
		settings = (settings != null) ? settings : HttpClientSettings.defaults();
		HttpClientTransport transport = createTransport(settings);
		this.httpClientTransportCustomizer.accept(transport);
		HttpClient httpClient = createHttpClient(settings, transport);
		PropertyMapper map = PropertyMapper.get();
		map.from(settings::connectTimeout).as(Duration::toMillis).to(httpClient::setConnectTimeout);
		map.from(settings::cookieHandling).as(this::asCookieStore).to(httpClient::setHttpCookieStore);
		map.from(settings::redirects).always().as(this::followRedirects).to(httpClient::setFollowRedirects);
		map.from(this.socketAddressResolver).to(httpClient::setSocketAddressResolver);
		this.customizer.accept(httpClient);
		return httpClient;
	}

	private HttpClient createHttpClient(HttpClientSettings settings, HttpClientTransport transport) {
		return (settings.readTimeout() != null || settings.inetAddressFilter() != null)
				? new CustomizedHttpClient(transport, settings) : new HttpClient(transport);
	}

	private HttpClientTransport createTransport(HttpClientSettings settings) {
		ClientConnector connector = createClientConnector(settings.sslBundle());
		HttpClientTransport clientTransport = this.httpClientTransportFactory.apply(connector);
		Assert.state(clientTransport != null, "'httpClientTransportFactory' did not return a client transport");
		return clientTransport;
	}

	private ClientConnector createClientConnector(@Nullable SslBundle sslBundle) {
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

	private @Nullable HttpCookieStore asCookieStore(HttpCookieHandling cookieHandling) {
		return switch (cookieHandling) {
			case ENABLE_WHEN_POSSIBLE, ENABLE -> new HttpCookieStore.Default();
			case DISABLE -> new HttpCookieStore.Empty();
		};
	}

	private boolean followRedirects(@Nullable HttpRedirects redirects) {
		if (redirects == null) {
			return true;
		}
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> true;
			case DONT_FOLLOW -> false;
		};
	}

	/**
	 * {@link HttpClient} subclass to support customization.
	 */
	static class CustomizedHttpClient extends HttpClient {

		private final HttpClientSettings settings;

		CustomizedHttpClient(HttpClientTransport transport, HttpClientSettings settings) {
			super(transport);
			this.settings = settings;
		}

		@Override
		public void setSocketAddressResolver(SocketAddressResolver resolver) {
			if (this.settings.inetAddressFilter() != null) {
				Assert.notNull(resolver, "'resolver' must not be null when addresses are filtered");
				resolver = new JettyFilteredSocketAddressResolver(resolver, this.settings.inetAddressFilter());
			}
			super.setSocketAddressResolver(resolver);
		}

		@Override
		public org.eclipse.jetty.client.Request newRequest(java.net.URI uri) {
			Request request = super.newRequest(uri);
			if (this.settings.readTimeout() != null) {
				request.timeout(this.settings.readTimeout().toMillis(), TimeUnit.MILLISECONDS);
			}
			return request;
		}

	}

}
