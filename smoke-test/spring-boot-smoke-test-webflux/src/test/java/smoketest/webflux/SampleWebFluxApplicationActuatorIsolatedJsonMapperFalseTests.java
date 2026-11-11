/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webflux;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration test for WebFlux actuator when using an isolated {@link JsonMapper}.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.endpoints.jackson.isolated-json-mapper=false",
				"spring.jackson.mapper.require-setters-for-getters=true" })
@ContextConfiguration(loader = ApplicationStartupSpringBootContextLoader.class)
@AutoConfigureWebTestClient
class SampleWebFluxApplicationActuatorIsolatedJsonMapperFalseTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void bodyIsEmptyDueToMainJsonMapperRequiringSettersForGetters() {
		this.webClient.get()
			.uri("/actuator/startup")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.json("{}");
	}

}
