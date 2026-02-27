/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.boot.webtestclient.autoconfigure;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with
 * {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient} timeout property
 * binding.
 *
 * @author Jay Choi
 */
@SpringBootTest(properties = { "spring.main.web-application-type=reactive",
		"spring.test.webtestclient.timeout=30s" }, classes = ExampleWebTestClientApplication.class)
@AutoConfigureWebTestClient
@EnableWebFlux
class WebTestClientTimeoutSpringBootTestIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void timeoutFromPropertyShouldBeApplied() {
		assertThat(this.webClient).hasFieldOrPropertyWithValue("responseTimeout", Duration.ofSeconds(30));
	}

}
