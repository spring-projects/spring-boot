/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * Configure {@link Builder RestClient.Builder} with sensible defaults.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code RestClient.Builder} whose configuration is based upon that produced by
 * auto-configuration.
 *
 * @author Moritz Halbritter
 * @since 4.0.0
 */
public class RestClientBuilderConfigurer {

	private final ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder;

	private final HttpClientSettings clientSettings;

	private final List<RestClientCustomizer> customizers;

	public RestClientBuilderConfigurer() {
		this(ClientHttpRequestFactoryBuilder.detect(), HttpClientSettings.defaults(), Collections.emptyList());
	}

	RestClientBuilderConfigurer(ClientHttpRequestFactoryBuilder<?> requestFactoryBuilder,
			HttpClientSettings clientSettings, List<RestClientCustomizer> customizers) {
		this.requestFactoryBuilder = requestFactoryBuilder;
		this.clientSettings = clientSettings;
		this.customizers = customizers;
	}

	/**
	 * Configure the specified {@link Builder RestClient.Builder}. The builder can be
	 * further tuned and default settings can be overridden.
	 * @param builder the {@link Builder RestClient.Builder} instance to configure
	 * @return the configured builder
	 */
	public RestClient.Builder configure(RestClient.Builder builder) {
		builder.requestFactory(this.requestFactoryBuilder.build(this.clientSettings));
		applyCustomizers(builder);
		return builder;
	}

	private void applyCustomizers(RestClient.Builder builder) {
		for (RestClientCustomizer customizer : this.customizers) {
			customizer.customize(builder);
		}
	}

}
