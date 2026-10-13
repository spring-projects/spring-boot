/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.secure.webflux;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration test for cors preflight requests to management endpoints.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("cors")
@AutoConfigureWebTestClient
class CorsSampleActuatorApplicationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void endpointShouldReturnUnauthorized() {
		this.webClient.get().uri("/actuator/env").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void preflightRequestToEndpointShouldReturnOk() {
		this.webClient.options()
			.uri("/actuator/env")
			.header("Origin", "http://localhost:8080")
			.header("Access-Control-Request-Method", "GET")
			.exchange()
			.expectStatus()
			.isOk();
	}

	@Test
	void preflightRequestWhenCorsConfigInvalidShouldReturnForbidden() {
		this.webClient.options()
			.uri("/actuator/env")
			.header("Origin", "http://localhost:9095")
			.header("Access-Control-Request-Method", "GET")
			.exchange()
			.expectStatus()
			.isForbidden();
	}

}
