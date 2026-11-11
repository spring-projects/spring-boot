/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webflux;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for WebFlux actuator when not using an isolated {@link JsonMapper}.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.endpoints.jackson.isolated-json-mapper=true",
				"spring.jackson.mapper.require-setters-for-getters=true" })
@ContextConfiguration(loader = ApplicationStartupSpringBootContextLoader.class)
@AutoConfigureWebTestClient
class SampleWebFluxApplicationActuatorIsolatedJsonMapperTrueTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void bodyIsPresentAsOnlyMainObjectMapperRequiresSettersForGetters() {
		this.webClient.get()
			.uri("/actuator/startup")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.consumeWith(this::assertExpectedJson);
	}

	private void assertExpectedJson(EntityExchangeResult<byte[]> result) {
		String body = new String(result.getResponseBody(), StandardCharsets.UTF_8);
		assertThat(body).contains("\"timeline\":");
	}

}
