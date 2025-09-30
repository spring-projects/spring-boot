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

package org.springframework.boot.web.server.test.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Integration test for {@link RestTestClientContextCustomizer} with a custom client.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RestTestClientContextCustomizerWithOverridePathIntegrationTests {

	@Autowired
	private RestTestClient webClient;

	@Test
	void test() {
		assertThat(this.webClient).isInstanceOf(CustomRestTestClient.class);
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ TestWebMvcConfiguration.class, NoRestTestClientBeanChecker.class })
	@RestController
	static class TestConfig {

		@GetMapping("/")
		String root() {
			return "hello";
		}

		@Bean
		CustomRestTestClient customRestTestClient() {
			return mock(CustomRestTestClient.class);
		}

	}

	interface CustomRestTestClient extends RestTestClient {

	}

}
