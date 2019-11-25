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

package smoketest.rsocket;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.rsocket.context.LocalRSocketServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.security.rsocket.metadata.BasicAuthenticationEncoder;
import org.springframework.security.rsocket.metadata.UsernamePasswordMetadata;

@SpringBootTest(properties = "spring.rsocket.server.port=0")
public class SampleRSocketApplicationTests {

	@LocalRSocketServerPort
	private int port;

	@Autowired
	private RSocketRequester.Builder builder;

	@Test
	void unauthenticatedAccessToRSocketEndpoint() {
		RSocketRequester requester = this.builder.connectTcp("localhost", this.port).block(Duration.ofSeconds(5));
		Mono<Project> result = requester.route("find.project.spring-boot").retrieveMono(Project.class);
		StepVerifier.create(result).expectErrorMessage("Access Denied").verify();
	}

	@Test
	void rSocketEndpoint() {
		RSocketRequester requester = this.builder
				.rsocketStrategies((builder) -> builder.encoder(new BasicAuthenticationEncoder()))
				.setupMetadata(new UsernamePasswordMetadata("user", "password"),
						UsernamePasswordMetadata.BASIC_AUTHENTICATION_MIME_TYPE)
				.connectTcp("localhost", this.port).block(Duration.ofSeconds(5));
		Mono<Project> result = requester.route("find.project.spring-boot").retrieveMono(Project.class);
		StepVerifier.create(result)
				.assertNext((project) -> Assertions.assertThat(project.getName()).isEqualTo("spring-boot"))
				.verifyComplete();
	}

}
