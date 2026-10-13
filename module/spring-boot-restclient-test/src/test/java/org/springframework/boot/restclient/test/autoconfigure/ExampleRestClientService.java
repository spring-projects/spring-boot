/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

/**
 * Example web client using {@code RestClient} with {@link RestClientTest @RestClientTest}
 * tests.
 *
 * @author Scott Frederick
 */
@Service
public class ExampleRestClientService {

	private final Builder builder;

	private final RestClient restClient;

	public ExampleRestClientService(RestClient.Builder builder) {
		this.builder = builder;
		this.restClient = builder.baseUrl("https://example.com").build();
	}

	protected Builder getRestClientBuilder() {
		return this.builder;
	}

	public @Nullable String test() {
		return this.restClient.get().uri("/test").retrieve().toEntity(String.class).getBody();
	}

	public void testPostWithBody(String body) {
		this.restClient.post().uri("/test").body(body).retrieve().toBodilessEntity();
	}

}
