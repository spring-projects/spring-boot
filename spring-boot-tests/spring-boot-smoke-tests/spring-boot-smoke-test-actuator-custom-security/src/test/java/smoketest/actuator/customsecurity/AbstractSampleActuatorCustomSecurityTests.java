/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.actuator.customsecurity;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for actuator tests with custom security.
 *
 * @author Madhura Bhave
 */
abstract class AbstractSampleActuatorCustomSecurityTests {

	abstract String getPath();

	abstract String getManagementPath();

	abstract Environment getEnvironment();

	@Test
	void homeIsSecure() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = restTemplate().getForEntity(getPath() + "/", Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(entity.getHeaders()).doesNotContainKey("Set-Cookie");
	}

	@Test
	void testInsecureStaticResources() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getPath() + "/css/bootstrap.min.css", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("body");
	}

	@Test
	void actuatorInsecureEndpoint() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getManagementPath() + "/actuator/health",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
		entity = restTemplate().getForEntity(getManagementPath() + "/actuator/health/diskSpace", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
	}

	@Test
	void actuatorLinksWithAnonymous() {
		ResponseEntity<Object> entity = restTemplate().getForEntity(getManagementPath() + "/actuator", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = restTemplate().getForEntity(getManagementPath() + "/actuator/", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void actuatorLinksWithUnauthorizedUser() {
		ResponseEntity<Object> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void actuatorLinksWithAuthorizedUser() {
		ResponseEntity<Object> entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		adminRestTemplate().getForEntity(getManagementPath() + "/actuator/", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void actuatorSecureEndpointWithAnonymous() {
		ResponseEntity<Object> entity = restTemplate().getForEntity(getManagementPath() + "/actuator/env",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = restTemplate().getForEntity(
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void actuatorSecureEndpointWithUnauthorizedUser() {
		ResponseEntity<Object> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/env",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = userRestTemplate().getForEntity(
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void actuatorSecureEndpointWithAuthorizedUser() {
		ResponseEntity<Object> entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator/env",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator/env/", Object.class);
		// EndpointRequest matches the trailing slash but MVC doesn't
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		entity = adminRestTemplate().getForEntity(
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include", Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void secureServletEndpointWithAnonymous() {
		ResponseEntity<String> entity = restTemplate().getForEntity("/actuator/se1", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = restTemplate().getForEntity(getManagementPath() + "/actuator/se1/list", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void secureServletEndpointWithUnauthorizedUser() {
		ResponseEntity<String> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/se1",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/se1/list", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void secureServletEndpointWithAuthorizedUser() {
		ResponseEntity<String> entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator/se1",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator/se1/list", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void actuatorCustomMvcSecureEndpointWithAnonymous() {
		ResponseEntity<String> entity = restTemplate()
				.getForEntity(getManagementPath() + "/actuator/example/echo?text={t}", String.class, "test");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void actuatorCustomMvcSecureEndpointWithUnauthorizedUser() {
		ResponseEntity<String> entity = userRestTemplate()
				.getForEntity(getManagementPath() + "/actuator/example/echo?text={t}", String.class, "test");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void actuatorCustomMvcSecureEndpointWithAuthorizedUser() {
		ResponseEntity<String> entity = adminRestTemplate()
				.getForEntity(getManagementPath() + "/actuator/example/echo?text={t}", String.class, "test");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("test");
		assertThat(entity.getHeaders().getFirst("echo")).isEqualTo("test");
	}

	@Test
	void actuatorExcludedFromEndpointRequestMatcher() {
		ResponseEntity<Object> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/mappings",
				Object.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	TestRestTemplate restTemplate() {
		return configure(new TestRestTemplate());
	}

	TestRestTemplate adminRestTemplate() {
		return configure(new TestRestTemplate("admin", "admin"));
	}

	TestRestTemplate userRestTemplate() {
		return configure(new TestRestTemplate("user", "password"));
	}

	TestRestTemplate beansRestTemplate() {
		return configure(new TestRestTemplate("beans", "beans"));
	}

	private TestRestTemplate configure(TestRestTemplate restTemplate) {
		restTemplate.setUriTemplateHandler(new LocalHostUriTemplateHandler(getEnvironment()));
		return restTemplate;
	}

}
