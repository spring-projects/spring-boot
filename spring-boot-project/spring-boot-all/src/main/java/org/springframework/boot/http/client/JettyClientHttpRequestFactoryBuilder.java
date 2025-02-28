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

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings.Redirects;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpRequestFactoryBuilder#jetty()}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @since 3.4.0
 */
public final class JettyClientHttpRequestFactoryBuilder
		extends AbstractClientHttpRequestFactoryBuilder<JettyClientHttpRequestFactory> {

	private final Consumer<HttpClient> httpClientCustomizer;

	private final Consumer<HttpClientTransport> httpClientTransportCustomizer;

	private final Consumer<ClientConnector> clientConnectorCustomizerCustomizer;

	JettyClientHttpRequestFactoryBuilder() {
		this(null, emptyCustomizer(), emptyCustomizer(), emptyCustomizer());
	}

	private JettyClientHttpRequestFactoryBuilder(List<Consumer<JettyClientHttpRequestFactory>> customizers,
			Consumer<HttpClient> httpClientCustomizer, Consumer<HttpClientTransport> httpClientTransportCustomizer,
			Consumer<ClientConnector> clientConnectorCustomizerCustomizer) {
		super(customizers);
		this.httpClientCustomizer = httpClientCustomizer;
		this.httpClientTransportCustomizer = httpClientTransportCustomizer;
		this.clientConnectorCustomizerCustomizer = clientConnectorCustomizerCustomizer;
	}

	@Override
	public JettyClientHttpRequestFactoryBuilder withCustomizer(Consumer<JettyClientHttpRequestFactory> customizer) {
		return new JettyClientHttpRequestFactoryBuilder(mergedCustomizers(customizer), this.httpClientCustomizer,
				this.httpClientTransportCustomizer, this.clientConnectorCustomizerCustomizer);
	}

	@Override
	public JettyClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<JettyClientHttpRequestFactory>> customizers) {
		return new JettyClientHttpRequestFactoryBuilder(mergedCustomizers(customizers), this.httpClientCustomizer,
				this.httpClientTransportCustomizer, this.clientConnectorCustomizerCustomizer);
	}

	/**
	 * Return a new {@link JettyClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link HttpClient}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link JettyClientHttpRequestFactoryBuilder} instance
	 */
	public JettyClientHttpRequestFactoryBuilder withHttpClientCustomizer(Consumer<HttpClient> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' must not be null");
		return new JettyClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientCustomizer.andThen(httpClientCustomizer), this.httpClientTransportCustomizer,
				this.clientConnectorCustomizerCustomizer);
	}

	/**
	 * Return a new {@link JettyClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link HttpClientTransport}.
	 * @param httpClientTransportCustomizer the customizer to apply
	 * @return a new {@link JettyClientHttpRequestFactoryBuilder} instance
	 */
	public JettyClientHttpRequestFactoryBuilder withHttpClientTransportCustomizer(
			Consumer<HttpClientTransport> httpClientTransportCustomizer) {
		Assert.notNull(httpClientTransportCustomizer, "'httpClientTransportCustomizer' must not be null");
		return new JettyClientHttpRequestFactoryBuilder(getCustomizers(), this.httpClientCustomizer,
				this.httpClientTransportCustomizer.andThen(httpClientTransportCustomizer),
				this.clientConnectorCustomizerCustomizer);
	}

	/**
	 * Return a new {@link JettyClientHttpRequestFactoryBuilder} that applies additional
	 * customization to the underlying {@link ClientConnector}.
	 * @param clientConnectorCustomizerCustomizer the customizer to apply
	 * @return a new {@link JettyClientHttpRequestFactoryBuilder} instance
	 */
	public JettyClientHttpRequestFactoryBuilder withClientConnectorCustomizerCustomizer(
			Consumer<ClientConnector> clientConnectorCustomizerCustomizer) {
		Assert.notNull(clientConnectorCustomizerCustomizer, "'clientConnectorCustomizerCustomizer' must not be null");
		return new JettyClientHttpRequestFactoryBuilder(getCustomizers(), this.httpClientCustomizer,
				this.httpClientTransportCustomizer,
				this.clientConnectorCustomizerCustomizer.andThen(clientConnectorCustomizerCustomizer));
	}

	@Override
	protected JettyClientHttpRequestFactory createClientHttpRequestFactory(ClientHttpRequestFactorySettings settings) {
		JettyClientHttpRequestFactory requestFactory = createRequestFactory(settings);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
		map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
		return requestFactory;
	}

	private JettyClientHttpRequestFactory createRequestFactory(ClientHttpRequestFactorySettings settings) {
		HttpClientTransport transport = createTransport(settings);
		this.httpClientTransportCustomizer.accept(transport);
		HttpClient httpClient = new HttpClient(transport);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::redirects).as(this::followRedirects).to(httpClient::setFollowRedirects);
		this.httpClientCustomizer.accept(httpClient);
		return new JettyClientHttpRequestFactory(httpClient);
	}

	private HttpClientTransport createTransport(ClientHttpRequestFactorySettings settings) {
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

	private boolean followRedirects(Redirects redirects) {
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> true;
			case DONT_FOLLOW -> false;
		};
	}

	static class Classes {

		static final String HTTP_CLIENT = "org.eclipse.jetty.client.HttpClient";

		static final boolean PRESENT = ClassUtils.isPresent(HTTP_CLIENT, null);

	}

}
