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

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.nio.AsyncClientConnectionManager;
import org.apache.hc.core5.http.nio.ssl.BasicClientTlsStrategy;
import org.apache.hc.core5.net.NamedEndpoint;
import org.apache.hc.core5.reactor.ssl.SSLSessionVerifier;
import org.apache.hc.core5.reactor.ssl.TlsDetails;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.http.client.reactive.HttpComponentsClientHttpConnector;

/**
 * {@link ClientHttpConnectorFactory} for {@link HttpComponentsClientHttpConnector}.
 *
 * @author Phillip Webb
 */
class HttpComponentsClientHttpConnectorFactory
		implements ClientHttpConnectorFactory<HttpComponentsClientHttpConnector> {

	@Override
	public HttpComponentsClientHttpConnector createClientHttpConnector(SslBundle sslBundle) {
		HttpAsyncClientBuilder builder = HttpAsyncClients.custom();
		if (sslBundle != null) {
			SslOptions options = sslBundle.getOptions();
			SSLContext sslContext = sslBundle.createSslContext();
			SSLSessionVerifier sessionVerifier = new SSLSessionVerifier() {

				@Override
				public TlsDetails verify(NamedEndpoint endpoint, SSLEngine sslEngine) throws SSLException {
					if (options.getCiphers() != null) {
						sslEngine.setEnabledCipherSuites(SslOptions.toArray(options.getCiphers()));
					}
					if (options.getEnabledProtocols() != null) {
						sslEngine.setEnabledProtocols(SslOptions.toArray(options.getEnabledProtocols()));
					}
					return null;
				}

			};
			BasicClientTlsStrategy tlsStrategy = new BasicClientTlsStrategy(sslContext, sessionVerifier);
			AsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
				.setTlsStrategy(tlsStrategy)
				.build();
			builder.setConnectionManager(connectionManager);
		}
		return new HttpComponentsClientHttpConnector(builder.build());
	}

}
