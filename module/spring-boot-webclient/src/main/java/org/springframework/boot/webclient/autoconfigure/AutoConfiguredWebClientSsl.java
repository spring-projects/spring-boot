/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.autoconfigure;

import java.util.function.Consumer;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
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

	private final HttpClientSettings settings;

	private final SslBundles sslBundles;

	AutoConfiguredWebClientSsl(ClientHttpConnectorBuilder<?> connectorBuilder, HttpClientSettings settings,
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
			HttpClientSettings settings = this.settings.withSslBundle(bundle);
			ClientHttpConnector connector = this.connectorBuilder.build(settings);
			builder.clientConnector(connector);
		};
	}

}
