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

package smoketest.web.secure;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for tests to ensure that the error page is accessible only to
 * authorized users.
 *
 * @author Madhura Bhave
 */
abstract class AbstractUnauthenticatedErrorPageTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	private final String pathPrefix;

	protected AbstractUnauthenticatedErrorPageTests(String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	@Test
	void testBadCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "wrongpassword")
				.exchange(this.pathPrefix + "/test", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Unauthorized");
	}

	@Test
	void testNoCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.exchange(this.pathPrefix + "/test",
				HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Unauthorized");
	}

	@Test
	void testPublicNotFoundPage() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.exchange(this.pathPrefix + "/public/notfound",
				HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Not Found");
	}

	@Test
	void testPublicNotFoundPageWithCorrectCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "password")
				.exchange(this.pathPrefix + "/public/notfound", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Not Found");
	}

	@Test
	void testPublicNotFoundPageWithBadCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "wrong")
				.exchange(this.pathPrefix + "/public/notfound", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Unauthorized");
	}

	@Test
	void testCorrectCredentialsWithControllerException() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "password")
				.exchange(this.pathPrefix + "/fail", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Internal Server Error");
	}

	@Test
	void testCorrectCredentials() {
		final ResponseEntity<String> response = this.testRestTemplate.withBasicAuth("username", "password")
				.exchange(this.pathPrefix + "/test", HttpMethod.GET, null, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("test");
	}

}
