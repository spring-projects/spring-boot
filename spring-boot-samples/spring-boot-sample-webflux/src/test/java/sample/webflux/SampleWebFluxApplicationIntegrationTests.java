/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.webflux;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Basic integration tests for WebFlux application.
 *
 * @author Brian Clozel
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleWebFluxApplicationIntegrationTests {

	@LocalServerPort
	private int port;

	private WebClient webClient;

	@Before
	public void setUp() throws Exception {
		this.webClient = WebClient.create("http://localhost:" + this.port);
	}

	@Test
	public void testWelcome() throws Exception {
		Mono<String> body = this.webClient
				.get().uri("/")
				.accept(MediaType.TEXT_PLAIN)
				.exchange()
				.then(response -> response.bodyToMono(String.class));

		StepVerifier.create(body)
				.expectNext("Hello World")
				.verifyComplete();
	}

}
