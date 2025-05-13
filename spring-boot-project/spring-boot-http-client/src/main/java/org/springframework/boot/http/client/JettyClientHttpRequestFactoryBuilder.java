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
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpClientTransport;
import org.eclipse.jetty.io.ClientConnector;

import org.springframework.boot.context.properties.PropertyMapper;
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

	private final JettyHttpClientBuilder httpClientBuilder;

	JettyClientHttpRequestFactoryBuilder() {
		this(null, new JettyHttpClientBuilder());
	}

	private JettyClientHttpRequestFactoryBuilder(List<Consumer<JettyClientHttpRequestFactory>> customizers,
			JettyHttpClientBuilder httpClientBuilder) {
		super(customizers);
		this.httpClientBuilder = httpClientBuilder;
	}

	@Override
	public JettyClientHttpRequestFactoryBuilder withCustomizer(Consumer<JettyClientHttpRequestFactory> customizer) {
		return new JettyClientHttpRequestFactoryBuilder(mergedCustomizers(customizer), this.httpClientBuilder);
	}

	@Override
	public JettyClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<JettyClientHttpRequestFactory>> customizers) {
		return new JettyClientHttpRequestFactoryBuilder(mergedCustomizers(customizers), this.httpClientBuilder);
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
				this.httpClientBuilder.withCustomizer(httpClientCustomizer));
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
		return new JettyClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withHttpClientTransportCustomizer(httpClientTransportCustomizer));
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
		return new JettyClientHttpRequestFactoryBuilder(getCustomizers(),
				this.httpClientBuilder.withClientConnectorCustomizerCustomizer(clientConnectorCustomizerCustomizer));
	}

	@Override
	protected JettyClientHttpRequestFactory createClientHttpRequestFactory(ClientHttpRequestFactorySettings settings) {
		HttpClient httpClient = this.httpClientBuilder.build(asHttpClientSettings(settings.withTimeouts(null, null)));
		JettyClientHttpRequestFactory requestFactory = new JettyClientHttpRequestFactory(httpClient);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
		map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
		return requestFactory;
	}

	static class Classes {

		static final String HTTP_CLIENT = "org.eclipse.jetty.client.HttpClient";

		static boolean present(ClassLoader classLoader) {
			return ClassUtils.isPresent(HTTP_CLIENT, classLoader);
		}

	}

}
