/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * A second example web client used with {@link RestClientTest @RestClientTest} tests.
 *
 * @author Phillip Webb
 */
@Service
public class AnotherExampleRestTemplateService {

	private final RestTemplate restTemplate;

	public AnotherExampleRestTemplateService(RestTemplateBuilder builder) {
		this.restTemplate = builder.rootUri("https://example.com").build();
	}

	protected RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

	public @Nullable String test() {
		return this.restTemplate.getForEntity("/test", String.class).getBody();
	}

}
