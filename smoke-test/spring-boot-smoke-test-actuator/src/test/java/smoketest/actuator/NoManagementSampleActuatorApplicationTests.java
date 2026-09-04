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

package smoketest.actuator;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for switching off management endpoints.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "management.server.port=-1" })
@AutoConfigureRestTestClient
class NoManagementSampleActuatorApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	@SuppressWarnings("unchecked")
	void testHome() {
		this.restTestClient.get()
			.uri("/")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(Map.class)
			.value((body) -> assertThat(body).containsEntry("message", "Hello Phil"));
	}

	@Test
	void testMetricsNotAvailable() {
		testHome(); // makes sure some requests have been made
		this.restTestClient.get()
			.uri("/metrics")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchange()
			.expectStatus()
			.isNotFound();
	}

}
