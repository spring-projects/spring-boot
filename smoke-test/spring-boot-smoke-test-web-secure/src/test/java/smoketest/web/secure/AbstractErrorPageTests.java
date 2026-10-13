/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.web.secure;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for tests to ensure that the error page is accessible only to
 * authorized users.
 *
 * @author Madhura Bhave
 */
@AutoConfigureTestRestTemplate
abstract class AbstractErrorPageTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	private final String pathPrefix;

	protected AbstractErrorPageTests(String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	@Test
	void testBadCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "wrongpassword")
			.exchange(this.pathPrefix + "/test", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse).isNull();
	}

	@Test
	void testNoCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.exchange(this.pathPrefix + "/test",
				HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse).isNull();
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
		assertThat(jsonResponse).isNull();
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
		response.getBody();
		assertThat(response.getBody()).isEqualTo("test");
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@RestController
		static class TestController {

			@GetMapping("/test")
			String test() {
				return "test";
			}

			@GetMapping("/fail")
			String fail() {
				throw new RuntimeException();
			}

		}

	}

}
