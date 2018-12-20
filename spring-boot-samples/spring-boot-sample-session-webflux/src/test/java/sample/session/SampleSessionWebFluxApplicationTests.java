/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.session;

import java.time.Duration;
import java.util.Base64;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SampleSessionWebFluxApplication}.
 *
 * @author Vedran Pavic
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "server.servlet.session.timeout:2", webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SampleSessionWebFluxApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private WebClient.Builder webClientBuilder;

	@Test
	public void userDefinedMappingsSecureByDefault() throws Exception {
		WebClient webClient = this.webClientBuilder
				.baseUrl("http://localhost:" + this.port + "/").build();
		ClientResponse response = webClient.get().header("Authorization", getBasicAuth())
				.exchange().block(Duration.ofSeconds(30));
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		ResponseCookie sessionCookie = response.cookies().getFirst("SESSION");
		String sessionId = response.bodyToMono(String.class)
				.block(Duration.ofSeconds(30));
		response = webClient.get().cookie("SESSION", sessionCookie.getValue()).exchange()
				.block(Duration.ofSeconds(30));
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.bodyToMono(String.class).block(Duration.ofSeconds(30)))
				.isEqualTo(sessionId);
		Thread.sleep(2000);
		response = webClient.get().cookie("SESSION", sessionCookie.getValue()).exchange()
				.block(Duration.ofSeconds(30));
		assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	private String getBasicAuth() {
		return "Basic " + Base64.getEncoder().encodeToString("user:password".getBytes());
	}

}
