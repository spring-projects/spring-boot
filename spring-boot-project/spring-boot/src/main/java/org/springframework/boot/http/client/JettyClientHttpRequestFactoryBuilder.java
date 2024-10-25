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
import org.springframework.http.client.JettyClientHttpRequestFactory;
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

	JettyClientHttpRequestFactoryBuilder() {
		this(null);
	}

	private JettyClientHttpRequestFactoryBuilder(List<Consumer<JettyClientHttpRequestFactory>> customizers) {
		super(customizers);
	}

	@Override
	public JettyClientHttpRequestFactoryBuilder withCustomizer(Consumer<JettyClientHttpRequestFactory> customizer) {
		return new JettyClientHttpRequestFactoryBuilder(mergedCustomizers(customizer));
	}

	@Override
	public JettyClientHttpRequestFactoryBuilder withCustomizers(
			Collection<Consumer<JettyClientHttpRequestFactory>> customizers) {
		return new JettyClientHttpRequestFactoryBuilder(mergedCustomizers(customizers));
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
		HttpClient httpClient = new HttpClient(transport);
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(settings::redirects).as(this::followRedirects).to(httpClient::setFollowRedirects);
		return new JettyClientHttpRequestFactory(httpClient);
	}

	private HttpClientTransport createTransport(ClientHttpRequestFactorySettings settings) {
		if (settings.sslBundle() == null) {
			return new HttpClientTransportOverHTTP();
		}
		ClientConnector connector = createClientConnector(settings.sslBundle());
		return new HttpClientTransportDynamic(connector);
	}

	private ClientConnector createClientConnector(SslBundle sslBundle) {
		SSLContext sslContext = sslBundle.createSslContext();
		SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
		sslContextFactory.setSslContext(sslContext);
		ClientConnector connector = new ClientConnector();
		connector.setSslContextFactory(sslContextFactory);
		return connector;
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
