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

package smoketest.freemarker;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for FreeMarker application.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleWebFreeMarkerApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testFreeMarkerTemplate() {
		this.restTestClient.get()
			.uri("/")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("Hello, Andy"));
	}

	@Test
	void testFreeMarkerErrorTemplate() {
		this.restTestClient.get()
			.uri("/does-not-exist")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("Something went wrong: 404 Not Found"));
	}

	@Test
	void templateErrorPageForSpecificStatusCode() {
		this.restTestClient.get()
			.uri("/insufficient-storage")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INSUFFICIENT_STORAGE)
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("We are out of storage"));
	}

}
