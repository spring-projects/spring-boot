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

import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTypeExcludeFilter;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration test for {@link WebFluxTest}. This test verifies that
 * {@link WebFluxTypeExcludeFilter} includes a user defined security configuration.
 *
 * @author Daniil Razorenov
 */
@MockBean(EchoHandler.class)
@WebFluxTest
@ActiveProfiles("custom-security")
class WebFluxTestWithCustomSecurityTests {

	@Autowired
	WebTestClient testClient;

	@Test
	void shouldBePermitted() {
		this.testClient.get().uri("/api/resource").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectBody(String.class).isEqualTo("test resource");
	}

	@Test
	void shouldBeAccessDenied() {
		this.testClient.post().uri("/api/resource").accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
				.isUnauthorized();
	}

	@Test
	void shouldBeAuthenticatedWithBasicAuth() {
		this.testClient.post().uri("/api/resource").accept(MediaType.APPLICATION_JSON)
				.header("Authorization", getBasicAuth()).exchange().expectStatus().isOk().expectBody(String.class)
				.isEqualTo("resource created by user");
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

}
