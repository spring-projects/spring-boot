/*
 * Copyright 2012-2021 the original author or authors.
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

package smoketest.secure.webflux;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration test for cors preflight requests to management endpoints.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("cors")
class CorsSampleActuatorApplicationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void endpointShouldReturnUnauthorized() {
		this.webClient.get().uri("/actuator/env").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void preflightRequestToEndpointShouldReturnOk() throws Exception {
		this.webClient.options().uri("/actuator/env").header("Origin", "http://localhost:8080")
				.header("Access-Control-Request-Method", "GET").exchange().expectStatus().isOk();
	}

	@Test
	void preflightRequestWhenCorsConfigInvalidShouldReturnForbidden() throws Exception {
		this.webClient.options().uri("/actuator/env").header("Origin", "http://localhost:9095")
				.header("Access-Control-Request-Method", "GET").exchange().expectStatus().isForbidden();
	}

}
