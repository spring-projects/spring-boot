/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.secure;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
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
@AutoConfigureTestRestTemplate
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
		assertThat(jsonResponse).isNotNull();
		assertThat(jsonResponse.get("error").asString()).isEqualTo("Unauthorized");
	}

	@Test
	void testNoCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.exchange(this.pathPrefix + "/test",
				HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse).isNotNull();
		assertThat(jsonResponse.get("error").asString()).isEqualTo("Unauthorized");
	}

	@Test
	void testPublicNotFoundPage() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.exchange(this.pathPrefix + "/public/notfound",
				HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse).isNotNull();
		assertThat(jsonResponse.get("error").asString()).isEqualTo("Not Found");
	}

	@Test
	void testPublicNotFoundPageWithCorrectCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "password")
			.exchange(this.pathPrefix + "/public/notfound", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse).isNotNull();
		assertThat(jsonResponse.get("error").asString()).isEqualTo("Not Found");
	}

	@Test
	void testPublicNotFoundPageWithBadCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "wrong")
			.exchange(this.pathPrefix + "/public/notfound", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse).isNotNull();
		assertThat(jsonResponse.get("error").asString()).isEqualTo("Unauthorized");
	}

	@Test
	void testCorrectCredentialsWithControllerException() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "password")
			.exchange(this.pathPrefix + "/fail", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse).isNotNull();
		assertThat(jsonResponse.get("error").asString()).isEqualTo("Internal Server Error");
	}

	@Test
	void testCorrectCredentials() {
		final ResponseEntity<String> response = this.testRestTemplate.withBasicAuth("username", "password")
			.exchange(this.pathPrefix + "/test", HttpMethod.GET, null, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("test");
	}

}
