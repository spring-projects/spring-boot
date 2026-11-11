/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestInitializer;

/**
 * {@link ClientHttpRequestInitializer} to apply customizations from the
 * {@link RestTemplateBuilder}.
 *
 * @author Dmytro Nosan
 * @author Ilya Lukyanovich
 * @deprecated since 4.2.0 for removal in 4.4.0 with no replacement
 */
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
class RestTemplateBuilderClientHttpRequestInitializer implements ClientHttpRequestInitializer {

	private final @Nullable BasicAuthentication basicAuthentication;

	private final Map<String, List<String>> defaultHeaders;

	private final Set<RestTemplateRequestCustomizer<?>> requestCustomizers;

	RestTemplateBuilderClientHttpRequestInitializer(@Nullable BasicAuthentication basicAuthentication,
			Map<String, List<String>> defaultHeaders, Set<RestTemplateRequestCustomizer<?>> requestCustomizers) {
		this.basicAuthentication = basicAuthentication;
		this.defaultHeaders = defaultHeaders;
		this.requestCustomizers = requestCustomizers;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void initialize(ClientHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		if (this.basicAuthentication != null) {
			this.basicAuthentication.applyTo(headers);
		}
		this.defaultHeaders.forEach(headers::putIfAbsent);
		LambdaSafe.callbacks(RestTemplateRequestCustomizer.class, this.requestCustomizers, request)
			.invoke((customizer) -> customizer.customize(request));
	}

}
