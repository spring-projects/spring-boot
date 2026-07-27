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
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Integration tests for separate management and main service ports with Actuator's MVC
 * {@link org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint
 * rest controller endpoints} and {@link ExceptionHandler exception handler}.
 *
 * @author Guirong Hu
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "management.endpoints.web.exposure.include=*", "management.server.port=0" })
class ManagementDifferentPortAndEndpointWithExceptionHandlerSampleActuatorApplicationTests {

	@LocalManagementPort
	private int managementPort;

	@Test
	void testExceptionHandlerRestControllerEndpoint() {
		RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.managementPort)
			.defaultHeaders((headers) -> headers.setBasicAuth("user", "password"))
			.build()
			.get()
			.uri("/actuator/exception")
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.EXPECTATION_FAILED)
			.expectBody(String.class)
			.isEqualTo("this is a custom exception body");
	}

}
