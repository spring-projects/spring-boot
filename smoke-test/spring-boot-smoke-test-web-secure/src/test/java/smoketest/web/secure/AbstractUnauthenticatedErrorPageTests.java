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

package smoketest.web.secure;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for tests to ensure that the error page is accessible only to
 * authorized users.
 *
 * @author Madhura Bhave
 */
@AutoConfigureRestTestClient
abstract class AbstractUnauthenticatedErrorPageTests {

	@Autowired
	private RestTestClient rest;

	private final String pathPrefix;

	protected AbstractUnauthenticatedErrorPageTests(String pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	@Test
	void testBadCredentials() {
		this.rest.get()
			.uri(this.pathPrefix + "/test")
			.headers((headers) -> headers.setBasicAuth("username", "wrongpassword"))
			.exchange()
			.expectStatus()
			.isUnauthorized()
			.expectBody(JsonNode.class)
			.value((body) -> {
				assertThat(body).isNotNull();
				assertThat(body.get("error").asString()).isEqualTo("Unauthorized");
			});
	}

	@Test
	void testNoCredentials() {
		this.rest.get()
			.uri(this.pathPrefix + "/test")
			.exchange()
			.expectStatus()
			.isUnauthorized()
			.expectBody(JsonNode.class)
			.value((body) -> {
				assertThat(body).isNotNull();
				assertThat(body.get("error").asString()).isEqualTo("Unauthorized");
			});
	}

	@Test
	void testPublicNotFoundPage() {
		this.rest.get()
			.uri(this.pathPrefix + "/public/notfound")
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectBody(JsonNode.class)
			.value((body) -> {
				assertThat(body).isNotNull();
				assertThat(body.get("error").asString()).isEqualTo("Not Found");
			});
	}

	@Test
	void testPublicNotFoundPageWithCorrectCredentials() {
		this.rest.get()
			.uri(this.pathPrefix + "/public/notfound")
			.headers((headers) -> headers.setBasicAuth("username", "password"))
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectBody(JsonNode.class)
			.value((body) -> {
				assertThat(body).isNotNull();
				assertThat(body.get("error").asString()).isEqualTo("Not Found");
			});
	}

	@Test
	void testPublicNotFoundPageWithBadCredentials() {
		this.rest.get()
			.uri(this.pathPrefix + "/public/notfound")
			.headers((headers) -> headers.setBasicAuth("username", "wrong"))
			.exchange()
			.expectStatus()
			.isUnauthorized()
			.expectBody(JsonNode.class)
			.value((body) -> {
				assertThat(body).isNotNull();
				assertThat(body.get("error").asString()).isEqualTo("Unauthorized");
			});
	}

	@Test
	void testCorrectCredentialsWithControllerException() {
		this.rest.get()
			.uri(this.pathPrefix + "/fail")
			.headers((headers) -> headers.setBasicAuth("username", "password"))
			.exchange()
			.expectStatus()
			.is5xxServerError()
			.expectBody(JsonNode.class)
			.value((body) -> {
				assertThat(body).isNotNull();
				assertThat(body.get("error").asString()).isEqualTo("Internal Server Error");
			});
	}

	@Test
	void testCorrectCredentials() {
		this.rest.get()
			.uri(this.pathPrefix + "/test")
			.headers((headers) -> headers.setBasicAuth("username", "password"))
			.exchangeSuccessfully()
			.expectBody(String.class)
			.isEqualTo("test");
	}

}
