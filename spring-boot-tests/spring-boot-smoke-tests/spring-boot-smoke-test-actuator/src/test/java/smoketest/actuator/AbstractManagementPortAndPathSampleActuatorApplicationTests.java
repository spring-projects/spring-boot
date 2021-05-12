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

package smoketest.actuator;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for integration tests with separate management and main service ports.
 *
 * @author Dave Syer
 */
abstract class AbstractManagementPortAndPathSampleActuatorApplicationTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Autowired
	private Environment environment;

	@Test
	void testHome() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(
				new TestRestTemplate("user", "password").getForEntity("http://localhost:" + this.port, Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody().get("message")).isEqualTo("Hello Phil");
	}

	@Test
	void testMetrics() {
		testHome(); // makes sure some requests have been made
		ResponseEntity<Map<String, Object>> entity = asMapEntity(new TestRestTemplate()
				.getForEntity("http://localhost:" + this.managementPort + "/admin/metrics", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void testHealth() {
		ResponseEntity<String> entity = new TestRestTemplate().withBasicAuth("user", "password")
				.getForEntity("http://localhost:" + this.managementPort + "/admin/health", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("{\"status\":\"UP\",\"groups\":[\"live\",\"ready\"]}");
	}

	@Test
	void testEnvNotFound() {
		String unknownProperty = "test-does-not-exist";
		assertThat(this.environment.containsProperty(unknownProperty)).isFalse();
		ResponseEntity<String> entity = new TestRestTemplate().withBasicAuth("user", "password").getForEntity(
				"http://localhost:" + this.managementPort + "/admin/env/" + unknownProperty, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void testMissing() {
		ResponseEntity<String> entity = new TestRestTemplate("user", "password")
				.getForEntity("http://localhost:" + this.managementPort + "/admin/missing", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(entity.getBody()).contains("\"status\":404");
	}

	@Test
	void testErrorPage() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(new TestRestTemplate("user", "password")
				.getForEntity("http://localhost:" + this.port + "/error", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(entity.getBody().get("status")).isEqualTo(999);
	}

	@Test
	void testManagementErrorPage() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(new TestRestTemplate("user", "password")
				.getForEntity("http://localhost:" + this.managementPort + "/error", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody().get("status")).isEqualTo(999);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <K, V> ResponseEntity<Map<K, V>> asMapEntity(ResponseEntity<Map> entity) {
		return (ResponseEntity) entity;
	}

}
