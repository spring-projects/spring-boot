/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.secure.jersey;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for actuator tests with custom security.
 *
 * @author Madhura Bhave
 */
@AutoConfigureTestRestTemplate
abstract class AbstractJerseySecureTests {

	abstract String getPath();

	abstract String getManagementPath();

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	void helloEndpointIsSecure() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getPath() + "/hello", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
		ResponseEntity<String> entity = restTemplate().getForEntity(getManagementPath() + "/actuator", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = restTemplate().getForEntity(getManagementPath() + "/actuator/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void actuatorLinksWithUnauthorizedUser() {
		ResponseEntity<String> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void actuatorLinksWithAuthorizedUser() {
		ResponseEntity<String> entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		adminRestTemplate().getForEntity(getManagementPath() + "/actuator/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void actuatorSecureEndpointWithAnonymous() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getManagementPath() + "/actuator/env",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = restTemplate().getForEntity(
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void actuatorSecureEndpointWithUnauthorizedUser() {
		ResponseEntity<String> entity = userRestTemplate().getForEntity(getManagementPath() + "/actuator/env",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = userRestTemplate().getForEntity(
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void actuatorSecureEndpointWithAuthorizedUser() {
		ResponseEntity<String> entity = adminRestTemplate().getForEntity(getManagementPath() + "/actuator/env",
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = adminRestTemplate().getForEntity(
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void secureServletEndpointWithAnonymous() {
		ResponseEntity<String> entity = restTemplate().getForEntity(getManagementPath() + "/actuator/se1",
				String.class);
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
	void actuatorExcludedFromEndpointRequestMatcher() {
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
