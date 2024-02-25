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

import java.util.function.Consumer;

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

	private final ClientHttpConnectorFactory<?> clientHttpConnectorFactory;

	private final SslBundles sslBundles;

	/**
	 * Constructs a new AutoConfiguredWebClientSsl with the specified
	 * clientHttpConnectorFactory and sslBundles.
	 * @param clientHttpConnectorFactory the clientHttpConnectorFactory to be used for
	 * creating the WebClient
	 * @param sslBundles the sslBundles containing the SSL certificates and configurations
	 */
	AutoConfiguredWebClientSsl(ClientHttpConnectorFactory<?> clientHttpConnectorFactory, SslBundles sslBundles) {
		this.clientHttpConnectorFactory = clientHttpConnectorFactory;
		this.sslBundles = sslBundles;
	}

	/**
	 * Returns a Consumer that configures the WebClient.Builder with SSL settings from the
	 * specified bundle.
	 * @param bundleName the name of the SSL bundle
	 * @return a Consumer that configures the WebClient.Builder with SSL settings
	 */
	@Override
	public Consumer<WebClient.Builder> fromBundle(String bundleName) {
		return fromBundle(this.sslBundles.getBundle(bundleName));
	}

	/**
	 * Creates a Consumer function that configures the WebClient.Builder with the provided
	 * SslBundle. The SslBundle contains the necessary SSL configuration for the
	 * WebClient.
	 * @param bundle the SslBundle containing the SSL configuration
	 * @return a Consumer function that configures the WebClient.Builder
	 */
	@Override
	public Consumer<WebClient.Builder> fromBundle(SslBundle bundle) {
		return (builder) -> {
			ClientHttpConnector connector = this.clientHttpConnectorFactory.createClientHttpConnector(bundle);
			builder.clientConnector(connector);
		};
	}

}
