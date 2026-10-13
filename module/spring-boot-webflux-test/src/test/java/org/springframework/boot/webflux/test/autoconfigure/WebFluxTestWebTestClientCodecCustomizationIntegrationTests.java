/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests for {@link WebFluxTest @WebFluxTest} to validate the {@link WebTestClient
 * WebTestClient's} codecs are customized.
 *
 * @author Andy Wilkinson
 */
@WebFluxTest(controllers = JsonController.class)
class WebFluxTestWebTestClientCodecCustomizationIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void shouldBeAbleToCreatePojoViaParametersModule() {
		this.webClient.get().uri("/json").exchange().expectStatus().isOk().expectBody(ExamplePojo.class);
	}

}
