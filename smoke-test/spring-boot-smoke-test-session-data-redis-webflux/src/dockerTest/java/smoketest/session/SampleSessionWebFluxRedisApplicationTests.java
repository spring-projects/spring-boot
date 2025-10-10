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

package smoketest.session;

import java.time.Duration;
import java.util.Base64;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.util.function.Tuples;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SampleSessionWebFluxRedisApplication}.
 *
 * @author Vedran Pavic
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringBootTest(properties = "spring.session.timeout:10", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class SampleSessionWebFluxRedisApplicationTests {

	@Container
	@ServiceConnection
	private static final RedisContainer redis = TestImage.container(RedisContainer.class);

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
			String sessionCookie = tuple.getT1();
			return client.get().cookie("SESSION", sessionCookie).exchangeToMono((response) -> {
				assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
				return response.bodyToMono(String.class)
					.doOnNext((sessionId) -> assertThat(sessionId).isEqualTo(tuple.getT2()))
					.thenReturn(sessionCookie);
			});
		})
			.delayElement(Duration.ofSeconds(10))
			.flatMap((sessionCookie) -> client.get().cookie("SESSION", sessionCookie).exchangeToMono((response) -> {
				assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
				return response.releaseBody();
			}))
			.block(Duration.ofSeconds(30));
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

}
