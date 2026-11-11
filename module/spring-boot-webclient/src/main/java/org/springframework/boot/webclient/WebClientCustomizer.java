/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient;

import org.springframework.web.reactive.function.client.WebClient;

/**
 * Callback interface that can be used to customize a
 * {@link org.springframework.web.reactive.function.client.WebClient.Builder
 * WebClient.Builder}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@FunctionalInterface
public interface WebClientCustomizer {

	/**
	 * Callback to customize a
	 * {@link org.springframework.web.reactive.function.client.WebClient.Builder
	 * WebClient.Builder} instance.
	 * @param webClientBuilder the client builder to customize
	 */
	void customize(WebClient.Builder webClientBuilder);

}
