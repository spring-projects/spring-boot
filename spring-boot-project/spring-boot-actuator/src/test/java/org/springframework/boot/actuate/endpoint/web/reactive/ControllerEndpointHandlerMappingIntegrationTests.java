/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web.reactive;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointAccessResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Integration tests for {@link ControllerEndpointHandlerMapping}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @deprecated since 3.3.5 in favor of {@code @Endpoint} and {@code @WebEndpoint} support
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.3.5", forRemoval = true)
class ControllerEndpointHandlerMappingIntegrationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner(
			AnnotationConfigReactiveWebServerApplicationContext::new)
		.withUserConfiguration(EndpointConfiguration.class, ExampleWebFluxEndpoint.class);

	@Test
	void getMapping() {
		this.contextRunner.run(withWebTestClient((webTestClient) -> webTestClient.get()
			.uri("/actuator/example/one")
			.accept(MediaType.TEXT_PLAIN)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
			.expectBody(String.class)
			.isEqualTo("One")));
	}

	@Test
	void getWithUnacceptableContentType() {
		this.contextRunner.run(withWebTestClient((webTestClient) -> webTestClient.get()
			.uri("/actuator/example/one")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.NOT_ACCEPTABLE)));
	}

	@Test
	void postMapping() {
		this.contextRunner.run(withWebTestClient((webTestClient) -> webTestClient.post()
			.uri("/actuator/example/two")
			.bodyValue(Collections.singletonMap("id", "test"))
			.exchange()
			.expectStatus()
			.isCreated()
			.expectHeader()
			.valueEquals(HttpHeaders.LOCATION, "/example/test")));
	}

	@Test
	void postMappingWithReadOnlyAccessRespondsWith404() {
		this.contextRunner.withPropertyValues("endpoint-access=READ_ONLY")
			.run(withWebTestClient((webTestClient) -> webTestClient.post()
				.uri("/actuator/example/two")
				.bodyValue(Collections.singletonMap("id", "test"))
				.exchange()
				.expectStatus()
				.isNotFound()));
	}

	@Test
	void getToRequestMapping() {
		this.contextRunner.run(withWebTestClient((webTestClient) -> webTestClient.get()
			.uri("/actuator/example/three")
			.accept(MediaType.TEXT_PLAIN)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
			.expectBody(String.class)
			.isEqualTo("Three")));
	}

	@Test
	void getToRequestMappingWithReadOnlyAccess() {
		this.contextRunner.withPropertyValues("endpoint-access=READ_ONLY")
			.run(withWebTestClient((webTestClient) -> webTestClient.get()
				.uri("/actuator/example/three")
				.accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus()
				.isOk()
				.expectHeader()
				.contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
				.expectBody(String.class)
				.isEqualTo("Three")));
	}

	@Test
	void postToRequestMapping() {
		this.contextRunner.run(withWebTestClient((webTestClient) -> webTestClient.post()
			.uri("/actuator/example/three")
			.accept(MediaType.TEXT_PLAIN)
			.exchange()
			.expectStatus()
			.isOk()
			.expectHeader()
			.contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
			.expectBody(String.class)
			.isEqualTo("Three")));
	}

	@Test
	void postToRequestMappingWithReadOnlyAccessRespondsWith405() {
		this.contextRunner.withPropertyValues("endpoint-access=READ_ONLY")
			.run(withWebTestClient((webTestClient) -> webTestClient.post()
				.uri("/actuator/example/three")
				.accept(MediaType.TEXT_PLAIN)
				.exchange()
				.expectStatus()
				.isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)));
	}

	private ContextConsumer<AssertableReactiveWebApplicationContext> withWebTestClient(
			Consumer<WebTestClient> webClient) {
		return (context) -> {
			int port = ((AnnotationConfigReactiveWebServerApplicationContext) context.getSourceApplicationContext())
				.getWebServer()
				.getPort();
			WebTestClient webTestClient = createWebTestClient(port);
			webClient.accept(webTestClient);
		};
	}

	private WebTestClient createWebTestClient(int port) {
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory("http://localhost:" + port);
		uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		return WebTestClient.bindToServer()
			.uriBuilderFactory(uriBuilderFactory)
			.responseTimeout(Duration.ofMinutes(5))
			.build();
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			WebFluxAutoConfiguration.class })
	static class EndpointConfiguration {

		@Bean
		NettyReactiveWebServerFactory netty() {
			return new NettyReactiveWebServerFactory(0);
		}

		@Bean
		HttpHandler httpHandler(ApplicationContext applicationContext) {
			return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		}

		@Bean
		ControllerEndpointDiscoverer webEndpointDiscoverer(ApplicationContext applicationContext) {
			return new ControllerEndpointDiscoverer(applicationContext, null, Collections.emptyList());
		}

		@Bean
		ControllerEndpointHandlerMapping webEndpointHandlerMapping(ControllerEndpointsSupplier endpointsSupplier,
				EndpointAccessResolver endpointAccessResolver) {
			return new ControllerEndpointHandlerMapping(new EndpointMapping("actuator"),
					endpointsSupplier.getEndpoints(), null, endpointAccessResolver);
		}

		@Bean
		EndpointAccessResolver endpointAccessResolver(Environment environment) {
			return (id, defaultAccess) -> environment.getProperty("endpoint-access", Access.class, Access.UNRESTRICTED);
		}

	}

	@RestControllerEndpoint(id = "example")
	static class ExampleWebFluxEndpoint {

		@GetMapping(path = "one", produces = MediaType.TEXT_PLAIN_VALUE)
		String one() {
			return "One";
		}

		@PostMapping("/two")
		ResponseEntity<String> two(@RequestBody Map<String, Object> content) {
			return ResponseEntity.created(URI.create("/example/" + content.get("id"))).build();
		}

		@RequestMapping(path = "/three", produces = MediaType.TEXT_PLAIN_VALUE)
		String three() {
			return "Three";
		}

	}

}
