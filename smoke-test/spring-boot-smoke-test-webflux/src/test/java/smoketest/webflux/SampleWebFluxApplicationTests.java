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

import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic tests for a WebFlux application, configuring the {@link WebTestClient} to test
 * without a running server.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest
@AutoConfigureWebTestClient
class SampleWebFluxApplicationTests {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<Map<String, Object>>() {
	};

	@Autowired
	private WebTestClient webClient;

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
	void testBadRequest() {
		Consumer<@Nullable Map<String, Object>> test = (content) -> assertThat(content).containsEntry("path",
				"/bad-request");
		this.webClient.get()
			.uri("/bad-request")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isBadRequest()
			.expectBody(MAP_TYPE)
			.value(test);
	}

	@Test
	void testServerError() {
		Consumer<@Nullable Map<String, Object>> test = (content) -> assertThat(content).containsEntry("path",
				"/five-hundred");
		this.webClient.get()
			.uri("/five-hundred")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
			.expectBody(MAP_TYPE)
			.value(test);
	}

}
