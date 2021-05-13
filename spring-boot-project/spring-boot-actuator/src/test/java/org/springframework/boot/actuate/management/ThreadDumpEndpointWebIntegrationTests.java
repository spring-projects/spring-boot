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

package org.springframework.boot.actuate.management;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ThreadDumpEndpoint} exposed by Jersey, Spring MVC, and
 * WebFlux.
 *
 * @author Andy Wilkinson
 */
class ThreadDumpEndpointWebIntegrationTests {

	@WebEndpointTest
	void getRequestWithJsonAcceptHeaderShouldProduceJsonThreadDumpResponse(WebTestClient client) {
		client.get().uri("/actuator/threaddump").accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON);
	}

	@WebEndpointTest
	void getRequestWithTextPlainAcceptHeaderShouldProduceTextPlainResponse(WebTestClient client) {
		String response = client.get().uri("/actuator/threaddump").accept(MediaType.TEXT_PLAIN).exchange()
				.expectStatus().isOk().expectHeader().contentType("text/plain;charset=UTF-8").expectBody(String.class)
				.returnResult().getResponseBody();
		assertThat(response).contains("Full thread dump");
	}

	@Configuration(proxyBeanMethods = false)
	public static class TestConfiguration {

		@Bean
		public ThreadDumpEndpoint endpoint() {
			return new ThreadDumpEndpoint();
		}

	}

}
