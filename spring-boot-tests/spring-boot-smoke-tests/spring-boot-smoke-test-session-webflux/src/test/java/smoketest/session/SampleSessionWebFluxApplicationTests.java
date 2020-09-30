/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.session;

import java.time.Duration;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import reactor.util.function.Tuples;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SampleSessionWebFluxApplication}.
 *
 * @author Vedran Pavic
 */
@SpringBootTest(properties = "server.servlet.session.timeout:2",
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleSessionWebFluxApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Test
	void userDefinedMappingsSecureByDefault() {
		WebClient client = this.webClientBuilder.baseUrl("http://localhost:" + this.port + "/").build();
		client.get().header("Authorization", getBasicAuth()).exchangeToMono((response) -> {
			assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
			return response.bodyToMono(String.class)
					.map((sessionId) -> Tuples.of(response.cookies().getFirst("SESSION").getValue(), sessionId));
		}).flatMap((tuple) -> {
			String sesssionCookie = tuple.getT1();
			return client.get().cookie("SESSION", sesssionCookie).exchangeToMono((response) -> {
				assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
				return response.bodyToMono(String.class)
						.doOnNext((sessionId) -> assertThat(sessionId).isEqualTo(tuple.getT2()))
						.thenReturn(sesssionCookie);
			});
		}).delayElement(Duration.ofSeconds(2))
				.flatMap((sessionCookie) -> client.get().cookie("SESSION", sessionCookie).exchangeToMono((response) -> {
					assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
					return response.releaseBody();
				})).block(Duration.ofSeconds(30));
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

}
