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

package smoketest.web.secure;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for error page that permits access to all.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = { AbstractErrorPageTests.TestConfiguration.class,
				UnauthenticatedErrorPageTests.SecurityConfiguration.class, SampleWebSecureApplication.class },
		properties = { "server.error.include-message=always", "spring.security.user.name=username",
				"spring.security.user.password=password" })
class UnauthenticatedErrorPageTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	void testBadCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "wrongpassword")
				.exchange("/test", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Unauthorized");
	}

	@Test
	void testNoCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.exchange("/test", HttpMethod.GET, null,
				JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Unauthorized");
	}

	@Test
	void testPublicNotFoundPage() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.exchange("/public/notfound", HttpMethod.GET,
				null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Not Found");
	}

	@Test
	void testPublicNotFoundPageWithCorrectCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "password")
				.exchange("/public/notfound", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Not Found");
	}

	@Test
	void testPublicNotFoundPageWithBadCredentials() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "wrong")
				.exchange("/public/notfound", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Unauthorized");
	}

	@Test
	void testCorrectCredentialsWithControllerException() {
		final ResponseEntity<JsonNode> response = this.testRestTemplate.withBasicAuth("username", "password")
				.exchange("/fail", HttpMethod.GET, null, JsonNode.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		JsonNode jsonResponse = response.getBody();
		assertThat(jsonResponse.get("error").asText()).isEqualTo("Internal Server Error");
	}

	@Test
	void testCorrectCredentials() {
		final ResponseEntity<String> response = this.testRestTemplate.withBasicAuth("username", "password")
				.exchange("/test", HttpMethod.GET, null, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isEqualTo("test");
	}

	@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
	static class SecurityConfiguration {

		@Bean
		SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
			http.authorizeRequests((requests) -> {
				requests.antMatchers("/error").permitAll();
				requests.antMatchers("/public/**").permitAll();
				requests.anyRequest().authenticated();
			});
			http.httpBasic();
			return http.build();
		}

	}

}
