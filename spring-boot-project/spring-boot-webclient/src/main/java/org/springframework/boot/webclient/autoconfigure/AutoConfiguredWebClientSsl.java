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

package org.springframework.boot.webclient.autoconfigure;

import java.util.function.Consumer;

import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * An auto-configured {@link WebClientSsl} implementation.
 *
 * @author Phillip Webb
 */
class AutoConfiguredWebClientSsl implements WebClientSsl {

	private final ClientHttpConnectorBuilder<?> connectorBuilder;

	private final ClientHttpConnectorSettings settings;

	private final SslBundles sslBundles;

	AutoConfiguredWebClientSsl(ClientHttpConnectorBuilder<?> connectorBuilder, ClientHttpConnectorSettings settings,
			SslBundles sslBundles) {
		this.connectorBuilder = connectorBuilder;
		this.settings = settings;
		this.sslBundles = sslBundles;
	}

	@Override
	public Consumer<WebClient.Builder> fromBundle(String bundleName) {
		return fromBundle(this.sslBundles.getBundle(bundleName));
	}

	@Override
	public Consumer<WebClient.Builder> fromBundle(SslBundle bundle) {
		return (builder) -> {
			ClientHttpConnectorSettings settings = this.settings.withSslBundle(bundle);
			ClientHttpConnector connector = this.connectorBuilder.build(settings);
			builder.clientConnector(connector);
		};
	}

}
