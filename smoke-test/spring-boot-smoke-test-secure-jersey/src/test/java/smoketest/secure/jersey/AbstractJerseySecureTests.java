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

package smoketest.secure.jersey;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for actuator tests with custom security.
 *
 * @author Madhura Bhave
 */
abstract class AbstractJerseySecureTests {

	private final RestClient restClient = RestClient.create();

	abstract String getPath();

	abstract String getManagementPath();

	@Test
	void helloEndpointIsSecure() {
		ResponseEntity<String> entity = getForEntity(restClient(), getPath() + "/hello");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void actuatorInsecureEndpoint() {
		ResponseEntity<String> entity = getForEntity(restClient(), getManagementPath() + "/actuator/health");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
		entity = getForEntity(restClient(), getManagementPath() + "/actuator/health/diskSpace");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).contains("\"status\":\"UP\"");
	}

	@Test
	void actuatorLinksWithAnonymous() {
		ResponseEntity<String> entity = getForEntity(restClient(), getManagementPath() + "/actuator");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = getForEntity(restClient(), getManagementPath() + "/actuator/");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void actuatorLinksWithUnauthorizedUser() {
		ResponseEntity<String> entity = getForEntity(userRestClient(), getManagementPath() + "/actuator");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = getForEntity(userRestClient(), getManagementPath() + "/actuator/");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void actuatorLinksWithAuthorizedUser() {
		ResponseEntity<String> entity = getForEntity(adminRestClient(), getManagementPath() + "/actuator");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		getForEntity(adminRestClient(), getManagementPath() + "/actuator/");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void actuatorSecureEndpointWithAnonymous() {
		ResponseEntity<String> entity = getForEntity(restClient(), getManagementPath() + "/actuator/env");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = getForEntity(restClient(),
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void actuatorSecureEndpointWithUnauthorizedUser() {
		ResponseEntity<String> entity = getForEntity(userRestClient(), getManagementPath() + "/actuator/env");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = getForEntity(userRestClient(),
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void actuatorSecureEndpointWithAuthorizedUser() {
		ResponseEntity<String> entity = getForEntity(adminRestClient(), getManagementPath() + "/actuator/env");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = getForEntity(adminRestClient(),
				getManagementPath() + "/actuator/env/management.endpoints.web.exposure.include");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void secureServletEndpointWithAnonymous() {
		ResponseEntity<String> entity = getForEntity(restClient(), getManagementPath() + "/actuator/se1");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		entity = getForEntity(restClient(), getManagementPath() + "/actuator/se1/list");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void secureServletEndpointWithUnauthorizedUser() {
		ResponseEntity<String> entity = getForEntity(userRestClient(), getManagementPath() + "/actuator/se1");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		entity = getForEntity(userRestClient(), getManagementPath() + "/actuator/se1/list");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void secureServletEndpointWithAuthorizedUser() {
		ResponseEntity<String> entity = getForEntity(adminRestClient(), getManagementPath() + "/actuator/se1");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		entity = getForEntity(adminRestClient(), getManagementPath() + "/actuator/se1/list");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void actuatorExcludedFromEndpointRequestMatcher() {
		ResponseEntity<String> entity = getForEntity(userRestClient(), getManagementPath() + "/actuator/mappings");
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	RestClient restClient() {
		return this.restClient;
	}

	RestClient adminRestClient() {
		return RestClient.builder().defaultHeaders((headers) -> headers.setBasicAuth("admin", "admin")).build();
	}

	RestClient userRestClient() {
		return RestClient.builder().defaultHeaders((headers) -> headers.setBasicAuth("user", "password")).build();
	}

	static ResponseEntity<String> getForEntity(RestClient client, String uri) {
		return client.get().uri(uri).retrieve().onStatus(HttpStatusCode::isError, (request, response) -> {
		}).toEntity(String.class);
	}

}
