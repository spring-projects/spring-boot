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

package smoketest.actuator.ui;

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
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.web.error.include-message=always" })
@AutoConfigureRestTestClient
class SampleActuatorUiApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void testHome() {
		this.restTestClient.get()
			.uri("/")
			.accept(MediaType.TEXT_HTML)
			.headers((headers) -> headers.setBasicAuth("user", getPassword()))
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("<title>Hello"));
	}

	@Test
	void testCss() {
		this.restTestClient.get()
			.uri("/css/bootstrap.min.css")
			.headers((headers) -> headers.setBasicAuth("user", getPassword()))
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("body"));
	}

	@Test
	void testMetrics() {
		this.restTestClient.get().uri("/actuator/metrics").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void testError() {
		this.restTestClient.get()
			.uri("/error")
			.accept(MediaType.TEXT_HTML)
			.headers((headers) -> headers.setBasicAuth("user", getPassword()))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("<html>")
				.contains("<body>")
				.contains("Please contact the operator with the above information"));
	}

	private String getPassword() {
		return "password";
	}

}
