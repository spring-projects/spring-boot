/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests for {@link WebFluxTest @WebFluxTest} to validate converters are discovered.
 *
 * @author Stephane Nicoll
 */
@WebFluxTest(controllers = ExampleController2.class)
class WebFluxTestConverterIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void shouldFindConverter() {
		UUID id = UUID.randomUUID();
		this.webClient.get()
			.uri("/two/" + id)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.isEqualTo(id + "two");
	}

}
