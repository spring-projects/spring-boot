/*
 * Copyright 2012-2019 the original author or authors.
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

package sample.secure.jersey;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for actuator tests with custom security.
 *
 * @author Madhura Bhave
 */
public abstract class AbstractJerseySecureTests {

	abstract String getPath();

	abstract String getManagementPath();

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	public void helloEndpointIsSecure() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getPath() + "/hello", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void actuatorInsecureEndpoint() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getManagementPath() + "/actuator/health",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
		entity = restTemplate().getForEntity(getManagementPath() + "/actuator/health/diskSpace", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
	}

	@Test
	public void actuatorLinksWithAnonymous() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getManagementPath() + "/actuator", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = restTemplate().getForEntity(getManagementPath() + "/actuator/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void actuatorLinksWithUnauthorizedUser() {
		ResponseEntity<String> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void actuatorLinksWithAuthorizedUser() {
		ResponseEntity<String> entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		adminRestTemplate().getForEntity(getManagementPath() + "/actuator/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void actuatorSecureEndpointWithAnonymous() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getManagementPath() + "/actuator/env",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = restTemplate().getForEntity(
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void actuatorSecureEndpointWithUnauthorizedUser() {
		ResponseEntity<String> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/env",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = userRestTemplate().getForEntity(
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void actuatorSecureEndpointWithAuthorizedUser() {
		ResponseEntity<String> entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator/env",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = adminRestTemplate().getForEntity(
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void secureServletEndpointWithAnonymous() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getManagementPath() + "/actuator/jolokia",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = restTemplate().getForEntity(getManagementPath() + "/actuator/jolokia/list", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	public void secureServletEndpointWithUnauthorizedUser() {
		ResponseEntity<String> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/jolokia",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/jolokia/list", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	public void secureServletEndpointWithAuthorizedUser() {
		ResponseEntity<String> entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator/jolokia",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator/jolokia/list", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	public void actuatorExcludedFromEndpointRequestMatcher() {
		ResponseEntity<String> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/mappings",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	TestRestTemplate restTemplate() {
		return this.testRestTemplate;
	}

	TestRestTemplate adminRestTemplate() {
		return this.testRestTemplate.withBasicAuth("admin", "admin");
	}

	TestRestTemplate userRestTemplate() {
		return this.testRestTemplate.withBasicAuth("user", "password");
	}

}
