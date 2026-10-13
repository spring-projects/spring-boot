/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests for {@link WebFluxTest @WebFluxTest} when no explicit controller is defined.
 *
 * @author Stephane Nicoll
 */
@WebFluxTest
class WebFluxTestAllControllersIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void shouldFindController1() {
		this.webClient.get().uri("/one").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("one");
	}

	@Test
	void shouldFindController2() {
		this.webClient.get().uri("/two").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("two");
	}

	@Test
	void webExceptionHandling() {
		this.webClient.get().uri("/one/error").exchange().expectStatus().isBadRequest();
	}

	@Test
	void shouldFindJsonController() {
		this.webClient.get().uri("/json").exchange().expectStatus().isOk();
	}

}
