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

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports with empty endpoint
 * base path.
 *
 * @author HaiTao Zhang
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "management.endpoints.web.base-path=/" })
class SampleWebFluxApplicationActuatorDifferentPortTests {

	@LocalManagementPort
	private int managementPort;

	@Test
	void linksEndpointShouldBeAvailable() {
		WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.build()
			.get()
			.uri("/")
			.headers((headers) -> headers.setBasicAuth("user", getPassword()))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.value((response) -> assertThat(response).contains("\"_links\""));
	}

	private String getPassword() {
		return "password";
	}

}
