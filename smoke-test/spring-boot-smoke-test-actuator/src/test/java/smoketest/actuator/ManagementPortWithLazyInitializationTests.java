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

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports when
 * lazy-initialization is enabled.
 *
 * @author Madhura Bhave
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "management.server.port=0", "spring.main.lazy-initialization=true" })
class ManagementPortWithLazyInitializationTests {

	@LocalManagementPort
	private int managementPort;

	@Test
	void testHealth() {
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.defaultHeaders((headers) -> headers.setBasicAuth("user", "password"))
			.build()
			.get()
			.uri("/actuator/health")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("\"status\":\"UP\""));
	}

}
