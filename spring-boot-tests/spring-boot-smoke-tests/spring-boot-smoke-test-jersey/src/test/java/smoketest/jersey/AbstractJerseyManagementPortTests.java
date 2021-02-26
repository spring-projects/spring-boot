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

package smoketest.jersey;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;
import smoketest.jersey.AbstractJerseyManagementPortTests.ResourceConfigConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	void resourceShouldBeAvailableOnMainPort() {
		ResponseEntity<String> entity = this.testRestTemplate.getForEntity("http://localhost:" + this.port + "/test",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("test");
	}

	@Test
	void resourceShouldNotBeAvailableOnManagementPort() {
		ResponseEntity<String> entity = this.testRestTemplate
				.getForEntity("http://localhost:" + this.managementPort + "/test", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void actuatorShouldBeAvailableOnManagementPort() {
		ResponseEntity<String> entity = this.testRestTemplate
				.getForEntity("http://localhost:" + this.managementPort + "/actuator/health", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void actuatorShouldNotBeAvailableOnMainPort() {
		ResponseEntity<String> entity = this.testRestTemplate
				.getForEntity("http://localhost:" + this.port + "/actuator/health", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@TestConfiguration
	static class ResourceConfigConfiguration {

		@Bean
		ResourceConfigCustomizer customizer() {
			return new ResourceConfigCustomizer() {
				@Override
				public void customize(ResourceConfig config) {
					config.register(TestEndpoint.class);
				}
			};
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
