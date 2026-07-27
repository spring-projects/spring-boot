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

package smoketest.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SampleServletApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	void testHomeIsSecure() {
		HttpStatusCode status = restClient().get()
			.uri("/")
			.accept(MediaType.APPLICATION_JSON)
			.exchange((request, response) -> response.getStatusCode());
		assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void testHome() {
		ResponseEntity<String> entity = restClient().get()
			.uri("/")
			.headers((headers) -> headers.setBasicAuth("user", getPassword()))
			.retrieve()
			.toEntity(String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	private RestClient restClient() {
		return RestClient.create("http://localhost:" + this.port);
	}

	private String getPassword() {
		return "password";
	}

}
