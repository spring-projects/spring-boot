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

package org.springframework.boot.jetty.autoconfigure;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.jetty.autoconfigure.AutoConfigureWebServerJettyReactiveTests.ReactiveConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.test.AutoConfigureWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureWebServer @AutoConfigureWebServer} with Jetty.
 *
 * @author Phillip Webb
 */
@SpringBootTest(classes = ReactiveConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.main.web-application-type=reactive")
@AutoConfigureWebServer
class AutoConfigureWebServerJettyReactiveTests {

	@LocalServerPort
	private int port;

	@Test
	void startsTestServer() {
		RestTestClient restClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
		assertThat(RestTestClientResponse.from(restClient.get().exchange())).bodyText().isEqualTo("hello");
	}

	@Configuration
	static class ReactiveConfiguration {

		@Bean
		HttpHandler httpHandler() {
			return (request, response) -> {
				response.setStatusCode(HttpStatus.OK);
				DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap("hello".getBytes(StandardCharsets.UTF_8));
				return response.writeWith(Mono.just(dataBuffer));
			};
		}

	}

}
