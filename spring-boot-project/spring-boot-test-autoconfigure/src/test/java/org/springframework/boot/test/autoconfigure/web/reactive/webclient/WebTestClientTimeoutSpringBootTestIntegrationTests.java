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

package org.springframework.boot.test.autoconfigure.web.reactive.webclient;

import java.time.Duration;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with
 * {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient} timeout property
 * binding.
 *
 * @author Jay Choi
 * @author Andy Wilkinson
 */
@SpringBootTest(properties = { "spring.main.web-application-type=reactive" }, classes = ExampleWebFluxApplication.class)
class WebTestClientTimeoutSpringBootTestIntegrationTests {

	@Nested
	@AutoConfigureWebTestClient
	@TestPropertySource(properties = "spring.test.webtestclient.timeout=30s")
	class TimeoutFromProperty {

		@Autowired
		private WebTestClient webClient;

		@Test
		void timeoutIsApplied() {
			assertThat(this.webClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofSeconds(30));
		}

	}

	@Nested
	@AutoConfigureWebTestClient(timeout = "25s")
	class TimeoutFromAnnotation {

		@Autowired
		private WebTestClient webClient;

		@Test
		void timeoutIsApplied() {
			assertThat(this.webClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofSeconds(25));
		}

	}

	@Nested
	@AutoConfigureWebTestClient
	class DefaultTimeout {

		@Autowired
		private WebTestClient webClient;

		@Test
		void timeoutIsApplied() {
			assertThat(this.webClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofSeconds(5));
		}

	}

}
