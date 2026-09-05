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

package smoketest.actuator.extension;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "spring.web.error.include-message=always" })
@AutoConfigureRestTestClient
class SampleActuatorExtensionApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void healthActuatorIsNotExposed() {
		this.restTestClient.get().uri("/actuator/health").exchange().expectStatus().isNotFound();
	}

	@Test
	void healthExtensionWithAuthHeaderIsDenied() {
		this.restTestClient.get().uri("/myextension/health").exchange().expectStatus().isUnauthorized();
	}

	@Test
	void healthExtensionWithAuthHeader() {
		RestTestClient restTestClient = this.restTestClient.mutate()
			.defaultHeader("Authorization", "Bearer secret")
			.build();
		restTestClient.get().uri("/myextension/health").exchange().expectStatus().isOk();
	}

}
