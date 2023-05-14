/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.http.HttpClientTransportOverHTTP;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.http.client.reactive.JettyResourceFactory;

/**
 * {@link ClientHttpConnectorFactory} for {@link JettyClientHttpConnector}.
 *
 * @author Phillip Webb
 */
class JettyClientHttpConnectorFactory implements ClientHttpConnectorFactory<JettyClientHttpConnector> {

	private final JettyResourceFactory jettyResourceFactory;

	JettyClientHttpConnectorFactory(JettyResourceFactory jettyResourceFactory) {
		this.jettyResourceFactory = jettyResourceFactory;
	}

	@Override
	public JettyClientHttpConnector createClientHttpConnector(SslBundle sslBundle) {
		SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
		if (sslBundle != null) {
			SslOptions options = sslBundle.getOptions();
			if (options.getCiphers() != null) {
				sslContextFactory.setIncludeCipherSuites(options.getCiphers());
				sslContextFactory.setExcludeCipherSuites();
			}
			if (options.getEnabledProtocols() != null) {
				sslContextFactory.setIncludeProtocols(options.getEnabledProtocols());
				sslContextFactory.setExcludeProtocols();
			}
			sslContextFactory.setSslContext(sslBundle.createSslContext());
		}
		ClientConnector connector = new ClientConnector();
		connector.setSslContextFactory(sslContextFactory);
		HttpClientTransportOverHTTP transport = new HttpClientTransportOverHTTP(connector);
		HttpClient httpClient = new HttpClient(transport);
		return new JettyClientHttpConnector(httpClient, this.jettyResourceFactory);
	}

}
