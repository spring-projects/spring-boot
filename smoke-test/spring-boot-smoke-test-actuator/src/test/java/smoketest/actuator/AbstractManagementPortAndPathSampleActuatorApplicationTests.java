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

package smoketest.actuator;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

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
	@SuppressWarnings("unchecked")
	void testHome() {
		appPortClient().get()
			.uri("/")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsEntry("message", "Hello Phil"));
	}

	@Test
	void testMetrics() {
		testHome(); // makes sure some requests have been made
		managementPortClient().get().uri("/admin/metrics").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void testHealth() {
		managementPortClient().get()
			.uri("/admin/health")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(String.class)
			.isEqualTo("{\"groups\":[\"comp\",\"live\",\"liveness\",\"readiness\",\"ready\"],\"status\":\"UP\"}");
	}

	@Test
	void testGroupWithComposite() {
		managementPortClient().get()
			.uri("/admin/health/comp")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains(
					"components\":{\"a\":{\"details\":{\"hello\":\"spring-a\"},\"status\":\"UP\"},\"c\":{\"details\":{\"hello\":\"spring-c\"},\"status\":\"UP\"}}"));
	}

	@Test
	void testEnvNotFound() {
		String unknownProperty = "test-does-not-exist";
		assertThat(this.environment.containsProperty(unknownProperty)).isFalse();
		managementPortClient().get()
			.uri("/admin/env/" + unknownProperty)
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchange()
			.expectStatus()
			.isNotFound();
	}

	@Test
	@SuppressWarnings("unchecked")
	void testMissing() {
		managementPortClient().get()
			.uri("/admin/missing")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":404"));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testErrorPage() {
		appPortClient().get()
			.uri("/error")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsEntry("status", 999));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testManagementErrorPage() {
		managementPortClient().get()
			.uri("/error")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsEntry("status", 999));
	}

	RestTestClient appPortClient() {
		return RestTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
	}

	RestTestClient managementPortClient() {
		return RestTestClient.bindToServer().baseUrl("http://localhost:" + this.managementPort).build();
	}

}
