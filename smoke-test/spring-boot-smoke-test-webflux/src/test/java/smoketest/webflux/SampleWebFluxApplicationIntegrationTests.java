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

package smoketest.webflux;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for WebFlux application.
 *
 * @author Brian Clozel
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "server.error.include-message=always")
@AutoConfigureWebTestClient
class SampleWebFluxApplicationIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	void testWelcome() {
		this.webClient.get()
			.uri("/")
			.accept(MediaType.TEXT_PLAIN)
			.exchange()
			.expectBody(String.class)
			.isEqualTo("Hello World");
	}

	@Test
	void testEcho() {
		this.webClient.post()
			.uri("/echo")
			.contentType(MediaType.TEXT_PLAIN)
			.accept(MediaType.TEXT_PLAIN)
			.body(Mono.just("Hello WebFlux!"), String.class)
			.exchange()
			.expectBody(String.class)
			.isEqualTo("Hello WebFlux!");
	}

	@Test
	void testActuatorStatus() {
		this.webClient.get()
			.uri("/actuator/health")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.json("{\"status\":\"UP\"}");
	}

	@Test
	void templated404ErrorPage() {
		Consumer<@Nullable String> test = (body) -> assertThat(body).isEqualToNormalizingNewlines("404 page\n");
		this.webClient.get()
			.uri("/404")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isNotFound()
			.expectBody(String.class)
			.value(test);
	}

	@Test
	void templated4xxErrorPage() {
		Consumer<@Nullable String> test = (body) -> assertThat(body).isEqualToNormalizingNewlines("4xx page\n");
		this.webClient.get()
			.uri("/bad-request")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody(String.class)
			.value(test);
	}

	@Test
	void htmlErrorPage() {
		Consumer<@Nullable String> test = (body) -> assertThat(body).contains("status: 500")
			.contains("message: Expected!");
		this.webClient.get()
			.uri("/five-hundred")
			.accept(MediaType.TEXT_HTML)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
			.expectBody(String.class)
			.value(test);
	}

}
