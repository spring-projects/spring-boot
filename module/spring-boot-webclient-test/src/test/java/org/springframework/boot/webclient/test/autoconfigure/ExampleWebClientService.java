/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example web client using {@code WebClient} with {@link WebClientTest @WebClientTest}
 * tests.
 *
 * @author Scott Frederick
 */
@Service
public class ExampleWebClientService {

	private final WebClient.Builder builder;

	private final WebClient webClient;

	public ExampleWebClientService(WebClient.Builder builder) {
		this.builder = builder;
		this.webClient = builder.build();
	}

	protected WebClient.Builder getWebClientBuilder() {
		return this.builder;
	}

	public @Nullable String test() {
		ResponseEntity<String> response = this.webClient.get().uri("/test").retrieve().toEntity(String.class).block();
		assertThat(response).isNotNull();
		return response.getBody();
	}

	public void testPostWithBody(String body) {
		this.webClient.post().uri("/test").bodyValue(body).retrieve().toBodilessEntity().block();
	}

}
