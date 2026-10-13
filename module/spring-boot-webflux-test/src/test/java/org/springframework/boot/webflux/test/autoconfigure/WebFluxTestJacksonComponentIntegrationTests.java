/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests for {@link WebFluxTest @WebFluxTest} to validate {@link JacksonComponent} beans
 * are discovered.
 *
 * @author Stephane Nicoll
 */
@WebFluxTest(controllers = ExampleController2.class)
class WebFluxTestJacksonComponentIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void shouldFindJacksonComponent() {
		this.webClient.post()
			.uri("/two/" + "1234abcd")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.json("{ \"identifier\": \"1234abcd\" }", JsonCompareMode.LENIENT);
	}

}
