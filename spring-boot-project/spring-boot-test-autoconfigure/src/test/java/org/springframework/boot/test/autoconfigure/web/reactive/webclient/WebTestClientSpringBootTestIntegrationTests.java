/*
 * Copyright 2012-2021 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} with
 * {@link AutoConfigureWebTestClient @AutoConfigureWebTestClient} (i.e. full integration
 * test).
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(properties = "spring.main.web-application-type=reactive", classes = {
		WebTestClientSpringBootTestIntegrationTests.TestConfiguration.class, ExampleWebFluxApplication.class })
@AutoConfigureWebTestClient
class WebTestClientSpringBootTestIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void shouldFindController1() {
		this.webClient.get().uri("/one").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("one");
	}

	@Test
	void shouldFindController2() {
		this.webClient.get().uri("/two").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("two");
	}

	@Test
	void shouldHaveRealService() {
		assertThat(this.applicationContext.getBeansOfType(ExampleRealService.class)).hasSize(1);
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
			http.authorizeExchange((exchanges) -> exchanges.anyExchange().permitAll());
			return http.build();
		}

	}

}
