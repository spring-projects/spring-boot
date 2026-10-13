/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Example web client using {@code RestTemplate} with
 * {@link RestClientTest @RestClientTest} tests.
 *
 * @author Phillip Webb
 * @deprecated since 4.2.0 for removal in 4.4.0
 */
@Service
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
public class ExampleRestTemplateService {

	private final RestTemplate restTemplate;

	public ExampleRestTemplateService(RestTemplateBuilder builder) {
		this.restTemplate = builder.baseUri("https://example.com").build();
	}

	protected RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

	public @Nullable String test() {
		return this.restTemplate.getForEntity("/test", String.class).getBody();
	}

	public void testPostWithBody(String body) {
		this.restTemplate.postForObject("/test", body, String.class);
	}

}
