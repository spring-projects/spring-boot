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

package org.springframework.boot.test.web.reactive.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration test for {@link WebTestClientContextCustomizer}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.main.web-application-type=reactive")
@DirtiesContext
public class WebTestClientContextCustomizerIntegrationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	public void test() {
		this.webTestClient.get().uri("/").exchange().expectBody(String.class).isEqualTo("hello");
	}

	@Configuration
	@Import({ TestHandler.class, NoWebTestClientBeanChecker.class })
	static class TestConfig {

		@Bean
		public TomcatReactiveWebServerFactory webServerFactory() {
			return new TomcatReactiveWebServerFactory(0);
		}

	}

	static class TestHandler implements HttpHandler {

		private static final DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			response.setStatusCode(HttpStatus.OK);
			return response.writeWith(Mono.just(factory.wrap("hello".getBytes())));
		}

	}

}
