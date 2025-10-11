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

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.io.ClientConnector;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.JettyHttpClientBuilder;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Builder for {@link ClientHttpConnectorBuilder#jetty()}.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public final class JettyClientHttpConnectorBuilder
		extends AbstractClientHttpConnectorBuilder<JettyClientHttpConnector> {

	private final JettyHttpClientBuilder httpClientBuilder;

	JettyClientHttpConnectorBuilder() {
		this(null, new JettyHttpClientBuilder());
	}

	private JettyClientHttpConnectorBuilder(@Nullable List<Consumer<JettyClientHttpConnector>> customizers,
			JettyHttpClientBuilder httpClientBuilder) {
		super(customizers);
		this.httpClientBuilder = httpClientBuilder;
	}

	@Override
	public JettyClientHttpConnectorBuilder withCustomizer(Consumer<JettyClientHttpConnector> customizer) {
		return new JettyClientHttpConnectorBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
	}

	@Override
	public JettyClientHttpConnectorBuilder withCustomizers(Collection<Consumer<JettyClientHttpConnector>> customizers) {
		return new JettyClientHttpConnectorBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
	}

	/**
	 * Return a new {@link JettyClientHttpConnectorBuilder} that uses the given factory to
	 * create the {@link HttpClientTransport}.
	 * @param httpClientTransportFactory the {@link HttpClientTransport} factory to use
	 * @return a new {@link JettyClientHttpConnectorBuilder} instance
	 * @since 4.0.0
	 */
	public JettyClientHttpConnectorBuilder withHttpClientTransportFactory(
			Function<ClientConnector, HttpClientTransport> httpClientTransportFactory) {
		Assert.notNull(httpClientTransportFactory, "'httpClientTransportFactory' must not be null");
		return new JettyClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withHttpClientTransportFactory(httpClientTransportFactory));
	}

	/**
	 * Return a new {@link JettyClientHttpConnectorBuilder} that applies additional
	 * customization to the underlying {@link HttpClient}.
	 * @param httpClientCustomizer the customizer to apply
	 * @return a new {@link JettyClientHttpConnectorBuilder} instance
	 */
	public JettyClientHttpConnectorBuilder withHttpClientCustomizer(Consumer<HttpClient> httpClientCustomizer) {
		Assert.notNull(httpClientCustomizer, "'httpClientCustomizer' must not be null");
		return new JettyClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withCustomizer(httpClientCustomizer));
	}

	/**
	 * Return a new {@link JettyClientHttpConnectorBuilder} that applies additional
	 * customization to the underlying {@link HttpClientTransport}.
	 * @param httpClientTransportCustomizer the customizer to apply
	 * @return a new {@link JettyClientHttpConnectorBuilder} instance
	 */
	public JettyClientHttpConnectorBuilder withHttpClientTransportCustomizer(
			Consumer<HttpClientTransport> httpClientTransportCustomizer) {
		Assert.notNull(httpClientTransportCustomizer, "'httpClientTransportCustomizer' must not be null");
		return new JettyClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withHttpClientTransportCustomizer(httpClientTransportCustomizer));
	}

	/**
	 * Return a new {@link JettyClientHttpConnectorBuilder} that applies additional
	 * customization to the underlying {@link ClientConnector}.
	 * @param clientConnectorCustomizerCustomizer the customizer to apply
	 * @return a new {@link JettyClientHttpConnectorBuilder} instance
	 */
	public JettyClientHttpConnectorBuilder withClientConnectorCustomizerCustomizer(
			Consumer<ClientConnector> clientConnectorCustomizerCustomizer) {
		Assert.notNull(clientConnectorCustomizerCustomizer, "'clientConnectorCustomizerCustomizer' must not be null");
		return new JettyClientHttpConnectorBuilder(getCustomizers(),
				this.httpClientBuilder.withClientConnectorCustomizerCustomizer(clientConnectorCustomizerCustomizer));
	}

	/**
	 * Return a new {@link JettyClientHttpConnectorBuilder} that applies the given
	 * customizer. This can be useful for applying pre-packaged customizations.
	 * @param customizer the customizer to apply
	 * @return a new {@link JettyClientHttpConnectorBuilder}
	 * @since 4.0.0
	 */
	public JettyClientHttpConnectorBuilder with(UnaryOperator<JettyClientHttpConnectorBuilder> customizer) {
		return customizer.apply(this);
	}

	@Override
	protected JettyClientHttpConnector createClientHttpConnector(HttpClientSettings settings) {
		HttpClient httpClient = this.httpClientBuilder.build(settings);
		return new JettyClientHttpConnector(httpClient);
	}

	static class Classes {

		static final String HTTP_CLIENT = "org.eclipse.jetty.client.HttpClient";

		static final String REACTIVE_REQUEST = "org.eclipse.jetty.reactive.client.ReactiveRequest";

		static boolean present(@Nullable ClassLoader classLoader) {
			return ClassUtils.isPresent(HTTP_CLIENT, classLoader)
					&& ClassUtils.isPresent(REACTIVE_REQUEST, classLoader);
		}

	}

}
