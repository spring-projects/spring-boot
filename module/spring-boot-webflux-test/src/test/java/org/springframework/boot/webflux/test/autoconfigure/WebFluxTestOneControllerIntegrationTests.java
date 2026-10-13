/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests for {@link WebFluxTest @WebFluxTest} when a specific controller is defined.
 *
 * @author Stephane Nicoll
 */
@WebFluxTest(controllers = ExampleController1.class)
class WebFluxTestOneControllerIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void shouldFindController() {
		this.webClient.get().uri("/one").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("one");
	}

	@Test
	void shouldNotScanOtherController() {
		this.webClient.get().uri("/two").exchange().expectStatus().isNotFound();
	}

}
