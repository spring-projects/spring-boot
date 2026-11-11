/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.autoconfigure;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClientCustomizer} to apply {@link HttpMessageConverter
 * HttpMessageConverters}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
public class HttpMessageConvertersRestClientCustomizer implements RestClientCustomizer {

	private final List<ClientHttpMessageConvertersCustomizer> customizers;

	public HttpMessageConvertersRestClientCustomizer(ClientHttpMessageConvertersCustomizer... customizers) {
		this(Arrays.asList(customizers));
	}

	public HttpMessageConvertersRestClientCustomizer(List<ClientHttpMessageConvertersCustomizer> customizers) {
		Assert.notNull(customizers, "customizers must not be null");
		this.customizers = customizers;
	}

	@Override
	public void customize(RestClient.Builder restClientBuilder) {
		restClientBuilder.configureMessageConverters(
				(builder) -> this.customizers.forEach((customizer) -> customizer.customize(builder)));
	}

}
