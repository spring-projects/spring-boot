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

package org.springframework.boot.web.server.test;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.test.AutoConfigureWebServerWebMvcIntegrationTests.TestController;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AutoConfigureWebServer @AutoConfigureWebServer} and Spring
 * MVC.
 *
 * @author Phillip Webb
 */
@SpringBootTest(classes = TestController.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebServer
@AutoConfigureWebMvc
@AutoConfigureRestTestClient
class AutoConfigureWebServerWebMvcIntegrationTests {

	@Test
	void embeddedWebServerWithSpringMvcIsAvailable(@Autowired RestTestClient restClient) {
		assertThat(RestTestClientResponse.from(restClient.get().uri("/hello").exchange())).hasStatusOk()
			.bodyText()
			.isEqualTo("hello");
	}

	@RestController
	static class TestController {

		@RequestMapping("/hello")
		String hello() {
			return "hello";
		}

	}

}
