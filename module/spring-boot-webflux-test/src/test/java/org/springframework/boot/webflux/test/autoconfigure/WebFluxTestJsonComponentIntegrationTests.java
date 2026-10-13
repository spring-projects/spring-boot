/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests for {@link WebFluxTest @WebFluxTest} to validate
 * {@link org.springframework.boot.jackson2.JsonComponent} beans are discovered.
 *
 * @author Stephane Nicoll
 */
@WebFluxTest(controllers = ExampleController2.class, properties = "spring.http.codecs.preferred-json-mapper=jackson2")
class WebFluxTestJsonComponentIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void shouldFindJsonComponent() {
		this.webClient.post()
			.uri("/two2/1234abcd")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.json("{ \"identifier2\": \"1234abcd\" }", JsonCompareMode.LENIENT);
	}

}
