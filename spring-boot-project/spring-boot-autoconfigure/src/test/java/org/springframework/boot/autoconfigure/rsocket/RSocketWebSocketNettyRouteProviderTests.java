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

package org.springframework.boot.autoconfigure.rsocket;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.codec.CodecsAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RSocketWebSocketNettyRouteProvider}.
 *
 * @author Brian Clozel
 */
class RSocketWebSocketNettyRouteProviderTests {

	@Test
	void webEndpointsShouldWork() {
		new ReactiveWebApplicationContextRunner(AnnotationConfigReactiveWebServerApplicationContext::new)
				.withConfiguration(
						AutoConfigurations.of(HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class,
								ErrorWebFluxAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
								JacksonAutoConfiguration.class, CodecsAutoConfiguration.class,
								RSocketStrategiesAutoConfiguration.class, RSocketServerAutoConfiguration.class,
								RSocketMessagingAutoConfiguration.class, RSocketRequesterAutoConfiguration.class))
				.withUserConfiguration(WebConfiguration.class)
				.withPropertyValues("spring.rsocket.server.transport=websocket",
						"spring.rsocket.server.mapping-path=/rsocket")
				.run((context) -> {
					ReactiveWebServerApplicationContext serverContext = (ReactiveWebServerApplicationContext) context
							.getSourceApplicationContext();
					RSocketRequester requester = createRSocketRequester(context, serverContext.getWebServer());
					TestProtocol rsocketResponse = requester.route("websocket").data(new TestProtocol("rsocket"))
							.retrieveMono(TestProtocol.class).block(Duration.ofSeconds(3));
					assertThat(rsocketResponse.getName()).isEqualTo("rsocket");
					WebTestClient client = createWebTestClient(serverContext.getWebServer());
					client.get().uri("/protocol").exchange().expectStatus().isOk().expectBody().jsonPath("name",
							"http");
				});
	}

	private WebTestClient createWebTestClient(WebServer server) {
		return WebTestClient.bindToServer().baseUrl("http://localhost:" + server.getPort()).build();
	}

	private RSocketRequester createRSocketRequester(ApplicationContext context, WebServer server) {
		int port = server.getPort();
		RSocketRequester.Builder builder = context.getBean(RSocketRequester.Builder.class);
		return builder.dataMimeType(MediaType.APPLICATION_CBOR)
				.websocket(URI.create("ws://localhost:" + port + "/rsocket"));
	}

	@Configuration(proxyBeanMethods = false)
	static class WebConfiguration {

		@Bean
		WebController webController() {
			return new WebController();
		}

		@Bean
		NettyReactiveWebServerFactory customServerFactory(RSocketWebSocketNettyRouteProvider routeProvider) {
			NettyReactiveWebServerFactory serverFactory = new NettyReactiveWebServerFactory(0);
			serverFactory.addRouteProviders(routeProvider);
			return serverFactory;
		}

	}

	@Controller
	static class WebController {

		@GetMapping(path = "/protocol", produces = MediaType.APPLICATION_JSON_VALUE)
		@ResponseBody
		TestProtocol testWebEndpoint() {
			return new TestProtocol("http");
		}

		@MessageMapping("websocket")
		TestProtocol testRSocketEndpoint() {
			return new TestProtocol("rsocket");
		}

	}

	public static class TestProtocol {

		private String name;

		TestProtocol() {
		}

		TestProtocol(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
