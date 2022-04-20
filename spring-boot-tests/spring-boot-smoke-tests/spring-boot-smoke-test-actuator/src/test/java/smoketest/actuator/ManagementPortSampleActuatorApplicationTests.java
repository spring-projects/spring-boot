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
import smoketest.actuator.ManagementPortSampleActuatorApplicationTests.CustomErrorAttributes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "management.endpoint.health.show-details=always" })
@Import(CustomErrorAttributes.class)
class ManagementPortSampleActuatorApplicationTests {

	@LocalServerPort
	private int port;

	@LocalManagementPort
	private int managementPort;

	@Autowired
	private CustomErrorAttributes errorAttributes;

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
				.getForEntity("http://localhost:" + this.managementPort + "/actuator/metrics", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void testHealth() {
		ResponseEntity<String> entity = new TestRestTemplate().withBasicAuth("user", "password")
				.getForEntity("http://localhost:" + this.managementPort + "/actuator/health", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
		assertThat(entity.getBody()).contains("\"example\"");
		assertThat(entity.getBody()).contains("\"counter\":42");
	}

	@Test
	void testErrorPage() {
		ResponseEntity<Map<String, Object>> entity = asMapEntity(new TestRestTemplate("user", "password")
				.getForEntity("http://localhost:" + this.managementPort + "/error", Map.class));
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody().get("status")).isEqualTo(999);
	}

	@Test
	void securityContextIsAvailableToErrorHandling() {
		this.errorAttributes.securityContext = null;
		ResponseEntity<Map<String, Object>> entity = asMapEntity(new TestRestTemplate("user", "password")
				.getForEntity("http://localhost:" + this.managementPort + "/404", Map.class));
		assertThat(this.errorAttributes.securityContext.getAuthentication()).isNotNull();
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(entity.getBody().get("status")).isEqualTo(404);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <K, V> ResponseEntity<Map<K, V>> asMapEntity(ResponseEntity<Map> entity) {
		return (ResponseEntity) entity;
	}

	static class CustomErrorAttributes extends DefaultErrorAttributes {

		private volatile SecurityContext securityContext;

		@Override
		public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
			this.securityContext = SecurityContextHolder.getContext();
			return super.getErrorAttributes(webRequest, options);
		}

	}

}
