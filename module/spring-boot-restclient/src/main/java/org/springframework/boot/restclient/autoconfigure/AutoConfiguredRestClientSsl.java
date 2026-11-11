/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.autoconfigure;

import java.util.function.Consumer;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * An auto-configured {@link RestClientSsl} implementation.
 *
 * @author Phillip Webb
 * @author Dmytro Nosan
 */
class AutoConfiguredRestClientSsl implements RestClientSsl {

	private final ClientHttpRequestFactoryBuilder<?> builder;

	private final HttpClientSettings settings;

	private final SslBundles sslBundles;

	AutoConfiguredRestClientSsl(ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder,
			HttpClientSettings settings, SslBundles sslBundles) {
		this.builder = clientHttpRequestFactoryBuilder;
		this.settings = settings;
		this.sslBundles = sslBundles;
	}

	@Override
	public Consumer<RestClient.Builder> fromBundle(String bundleName) {
		return fromBundle(this.sslBundles.getBundle(bundleName));
	}

	@Override
	public Consumer<RestClient.Builder> fromBundle(SslBundle bundle) {
		return (builder) -> builder.requestFactory(requestFactory(bundle));
	}

	private ClientHttpRequestFactory requestFactory(SslBundle bundle) {
		return this.builder.build(this.settings.withSslBundle(bundle));
	}

}
