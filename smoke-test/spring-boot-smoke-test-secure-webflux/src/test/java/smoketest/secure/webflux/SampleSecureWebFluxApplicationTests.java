/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.secure.webflux;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for a secure reactive application.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "management.endpoint.health.show-details=never")
@AutoConfigureWebTestClient
class SampleSecureWebFluxApplicationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void userDefinedMappingsSecureByDefault() {
		this.webClient.get()
			.uri("/")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void healthInsecureByDefault() {
		this.webClient.get()
			.uri("/actuator/health")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isOk();
	}

	@Test
	void otherActuatorsSecureByDefault() {
		this.webClient.get()
			.uri("/actuator/env")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isUnauthorized();
	}

	@Test
	void userDefinedMappingsAccessibleOnLogin() {
		this.webClient.get()
			.uri("/")
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", getBasicAuth())
			.exchange()
			.expectBody(String.class)
			.isEqualTo("Hello user");
	}

	@Test
	void actuatorsAccessibleOnLogin() {
		this.webClient.get()
			.uri("/actuator/health")
			.accept(MediaType.APPLICATION_JSON)
			.header("Authorization", getBasicAuth())
			.exchange()
			.expectBody(String.class)
			.isEqualTo("{\"groups\":[\"liveness\",\"readiness\"],\"status\":\"UP\"}");
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

}
