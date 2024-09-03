/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docs.testing.springbootapplications.withmockenvironment

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.servlet.assertj.MockMvcTester

@SpringBootTest
@AutoConfigureMockMvc
class MyMockMvcTests {

	@Test
	fun testWithMockMvc(@Autowired mvc: MockMvcTester) {
		assertThat(mvc.get().uri("/")).hasStatusOk()
				.hasBodyTextEqualTo("Hello World")
	}

	// If Spring WebFlux is on the classpath, you can drive MVC tests with a WebTestClient

	@Test
	fun testWithWebTestClient(@Autowired webClient: WebTestClient) {
		webClient
				.get().uri("/")
				.exchange()
				.expectStatus().isOk
				.expectBody<String>().isEqualTo("Hello World")
	}

}
