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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Basic integration tests for WebFlux application.
 *
 * @author Brian Clozel
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleWebFluxApplicationIntegrationTests {

	@Autowired
	private WebTestClient webClient;

	@Test
	public void testWelcome() throws Exception {
		this.webClient.get().uri("/").accept(MediaType.TEXT_PLAIN).exchange()
				.expectBody(String.class).value().isEqualTo("Hello World");
	}

	@Test
	public void testMono() throws Exception {
		this.webClient
				.get().uri("/mono")
				.exchange()
				.expectBody(String.class).value().isEqualTo("mono");
	}

	@Test
	public void testFlux() throws Exception {
		this.webClient
				.get().uri("/flux")
				.exchange()
				.expectBody(String.class).value().isEqualTo(
						Stream.of("flux-one", "flux-two", "flux-three")
								.collect(Collectors.joining()));
	}
}
