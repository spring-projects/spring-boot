/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient;

import org.springframework.web.client.RestClient;

/**
 * Callback interface that can be used to customize a
 * {@link org.springframework.web.client.RestClient.Builder RestClient.Builder}.
 *
 * @author Arjen Poutsma
 * @since 4.0.0
 */
@FunctionalInterface
public interface RestClientCustomizer {

	/**
	 * Callback to customize a {@link org.springframework.web.client.RestClient.Builder
	 * RestClient.Builder} instance.
	 * @param restClientBuilder the client builder to customize
	 */
	void customize(RestClient.Builder restClientBuilder);

}
