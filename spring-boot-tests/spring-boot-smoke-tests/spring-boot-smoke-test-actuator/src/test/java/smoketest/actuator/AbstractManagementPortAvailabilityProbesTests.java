/*
 * Copyright 2012-2023 the original author or authors.
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

package smoketest.actuator;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for integration tests for adding Health probes additional paths.
 *
 * @author Ivo Smid
 */
abstract class AbstractManagementPortAvailabilityProbesTests {

	@LocalServerPort
	protected int port;

	@LocalManagementPort
	protected int managementPort;

	@Test
	void testLiveness() {
		ResponseEntity<String> entity = callHttpGetManagement("/actuator/health/liveness");
		assertUp(entity);
	}

	@Test
	void testReadiness() {
		ResponseEntity<String> entity = callHttpGetManagement("/actuator/health/readiness");
		assertUp(entity);
	}

	protected ResponseEntity<String> callHttpGetManagement(String urlPath) {
		return callHttpGet(urlPath, this.managementPort, Function.identity());
	}

	protected ResponseEntity<String> callHttpGetServer(String urlPath) {
		return callHttpGet(urlPath, this.port, (it) -> it.withBasicAuth("user", "password"));
	}

	private ResponseEntity<String> callHttpGet(String urlPath, int port,
			Function<TestRestTemplate, TestRestTemplate> customizer) {
		var testRestTemplate = customizer.apply(new TestRestTemplate());
		return testRestTemplate.getForEntity("http://localhost:" + port + urlPath, String.class);
	}

	protected static void assertUp(ResponseEntity<String> entity) {
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
	}

}
