/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smoketest.webflux;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for WebFlux actuator when not using an isolated {@link ObjectMapper}.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "management.endpoints.jackson.isolated-object-mapper=true",
				"spring.jackson.mapper.require-setters-for-getters=true" })
@ContextConfiguration(loader = ApplicationStartupSpringBootContextLoader.class)
@AutoConfigureWebTestClient
class SampleWebFluxApplicationActuatorIsolatedObjectMapperTrueTests {

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
