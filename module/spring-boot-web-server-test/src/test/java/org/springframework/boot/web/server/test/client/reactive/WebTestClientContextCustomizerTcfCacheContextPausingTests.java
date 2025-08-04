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

package org.springframework.boot.web.server.test.client.reactive;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.reactive.TomcatReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ContextPathCompositeHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Tests for {@link WebTestClientContextCustomizer} when the test context framework pauses
 * a context while it's in the cache.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.main.web-application-type=none")
class WebTestClientContextCustomizerTcfCacheContextPausingTests {

	@Nested
	@Import(TestConfig.class)
	@TestPropertySource(properties = { "context=one", "spring.main.web-application-type=reactive" })
	class ContextOne {

		@Autowired
		private WebTestClient webTestClient;

		@Test
		void test() {
			this.webTestClient.get().uri("/test").exchange().expectBody(String.class).isEqualTo("hello world");
		}

	}

	@Nested
	@Import(TestConfig.class)
	@TestPropertySource(properties = { "context=two", "spring.main.web-application-type=reactive" })
	class ContextTwo {

		@Autowired
		private WebTestClient webTestClient;

		@Test
		void test() {
			this.webTestClient.get().uri("/test").exchange().expectBody(String.class).isEqualTo("hello world");
		}

	}

	@Nested
	@Import(TestConfig.class)
	@TestPropertySource(properties = { "context=one", "spring.main.web-application-type=reactive" })
	class ReuseContextOne {

		@Autowired
		private WebTestClient webTestClient;

		@Test
		void test() {
			this.webTestClient.get().uri("/test").exchange().expectBody(String.class).isEqualTo("hello world");
		}

	}

	@SpringBootConfiguration(proxyBeanMethods = false)
	static class TestConfig {

		@Bean
		TomcatReactiveWebServerFactory webServerFactory() {
			return new TomcatReactiveWebServerFactory(0);
		}

		@Bean
		HttpHandler httpHandler() {
			TestHandler httpHandler = new TestHandler();
			Map<String, HttpHandler> handlersMap = Collections.singletonMap("/test", httpHandler);
			return new ContextPathCompositeHandler(handlersMap);
		}

	}

	static class TestHandler implements HttpHandler {

		private static final DefaultDataBufferFactory factory = new DefaultDataBufferFactory();

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			response.setStatusCode(HttpStatus.OK);
			return response.writeWith(Mono.just(factory.wrap("hello world".getBytes())));
		}

	}

}
