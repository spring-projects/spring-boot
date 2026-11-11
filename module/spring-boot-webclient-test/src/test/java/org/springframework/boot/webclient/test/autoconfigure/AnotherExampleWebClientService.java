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
 * A second example web client used with {@link WebClientTest @WebClientTest} tests.
 *
 * @author Scott Frederick
 */
@Service
public class AnotherExampleWebClientService {

	private final WebClient.Builder builder;

	private final WebClient webClient;

	public AnotherExampleWebClientService(WebClient.Builder builder) {
		this.builder = builder;
		this.webClient = builder.baseUrl("https://example.com").build();
	}

	protected WebClient.Builder geWebClientBuilder() {
		return this.builder;
	}

	public @Nullable String test() {
		ResponseEntity<String> response = this.webClient.get().uri("/test").retrieve().toEntity(String.class).block();
		assertThat(response).isNotNull();
		return response.getBody();
	}

}
