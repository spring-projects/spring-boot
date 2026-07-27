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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for endpoints configuration.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.mvc.servlet.path=/spring" })
@AutoConfigureRestTestClient
class ServletPathSampleActuatorApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	@SuppressWarnings("unchecked")
	void testErrorPath() {
		this.restTestClient.get()
			.uri("/spring/error")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
			.expectBody(Map.class)
			.value((body) -> {
				assertThat(body).containsEntry("error", "None");
				assertThat(body).containsEntry("status", 999);
			});
	}

	@Test
	void testHealth() {
		this.restTestClient.get()
			.uri("/spring/actuator/health")
			.headers((headers) -> headers.setBasicAuth("user", "password"))
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":\"UP\""));
	}

	@Test
	void testHomeIsSecure() {
		this.restTestClient.get()
			.uri("/spring/")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isUnauthorized()
			.expectHeader()
			.doesNotExist("Set-Cookie");
	}

}
