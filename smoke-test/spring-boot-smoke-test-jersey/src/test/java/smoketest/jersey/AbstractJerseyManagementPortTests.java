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

package smoketest.jersey;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;
import smoketest.jersey.AbstractJerseyManagementPortTests.ResourceConfigConfiguration;

import org.springframework.boot.jersey.autoconfigure.ResourceConfigCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for integration tests for Jersey using separate management and main service
 * ports.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.server.port=0")
@Import(ResourceConfigConfiguration.class)
class AbstractJerseyManagementPortTests {

	private final RestClient restClient = RestClient.create();

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Test
	void resourceShouldBeAvailableOnMainPort() {
		ResponseEntity<String> entity = getForEntity("http://localhost:" + this.port + "/test");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("test");
	}

	@Test
	void resourceShouldNotBeAvailableOnManagementPort() {
		ResponseEntity<String> entity = getForEntity("http://localhost:" + this.managementPort + "/test");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void actuatorShouldBeAvailableOnManagementPort() {
		ResponseEntity<String> entity = getForEntity("http://localhost:" + this.managementPort + "/actuator/health");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void actuatorShouldNotBeAvailableOnMainPort() {
		ResponseEntity<String> entity = getForEntity("http://localhost:" + this.port + "/actuator/health");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	private ResponseEntity<String> getForEntity(String uri) {
		return this.restClient.get().uri(uri).retrieve().onStatus(HttpStatusCode::isError, (request, response) -> {
		}).toEntity(String.class);
	}

	@TestConfiguration
	static class ResourceConfigConfiguration {

		@Bean
		ResourceConfigCustomizer customizer() {
			return (config) -> config.register(TestEndpoint.class);
		}

		@Path("/test")
		public static class TestEndpoint {

			@GET
			public String test() {
				return "test";
			}

		}

	}

}
