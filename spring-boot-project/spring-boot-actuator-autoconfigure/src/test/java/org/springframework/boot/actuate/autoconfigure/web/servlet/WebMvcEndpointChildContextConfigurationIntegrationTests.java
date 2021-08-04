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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link WebMvcEndpointChildContextConfiguration}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class WebMvcEndpointChildContextConfigurationIntegrationTests {

	private final WebApplicationContextRunner runner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
					.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
							ServletWebServerFactoryAutoConfiguration.class,
							ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
							EndpointAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
							ErrorMvcAutoConfiguration.class))
					.withUserConfiguration(SucceedingEndpoint.class, FailingEndpoint.class,
							FailingControllerEndpoint.class)
					.withInitializer(new ServerPortInfoApplicationContextInitializer())
					.withPropertyValues("server.port=0", "management.server.port=0",
							"management.endpoints.web.exposure.include=*", "server.error.include-exception=true",
							"server.error.include-message=always", "server.error.include-binding-errors=always");

	@Test // gh-17938
	void errorEndpointIsUsedWithEndpoint() {
		this.runner.run(withWebTestClient((client) -> {
			Map<String, ?> body = client.get().uri("actuator/fail").accept(MediaType.APPLICATION_JSON)
					.exchangeToMono(toResponseBody()).block();
			assertThat(body).hasEntrySatisfying("exception",
					(value) -> assertThat(value).asString().contains("IllegalStateException"));
			assertThat(body).hasEntrySatisfying("message",
					(value) -> assertThat(value).asString().contains("Epic Fail"));
		}));
	}

	@Test
	void errorPageAndErrorControllerIncludeDetails() {
		this.runner.withPropertyValues("server.error.include-stacktrace=always", "server.error.include-message=always")
				.run(withWebTestClient((client) -> {
					Map<String, ?> body = client.get().uri("actuator/fail").accept(MediaType.APPLICATION_JSON)
							.exchangeToMono(toResponseBody()).block();
					assertThat(body).hasEntrySatisfying("message",
							(value) -> assertThat(value).asString().contains("Epic Fail"));
					assertThat(body).hasEntrySatisfying("trace", (value) -> assertThat(value).asString()
							.contains("java.lang.IllegalStateException: Epic Fail"));
				}));
	}

	@Test
	void errorEndpointIsUsedWithRestControllerEndpoint() {
		this.runner.run(withWebTestClient((client) -> {
			Map<String, ?> body = client.get().uri("actuator/failController").accept(MediaType.APPLICATION_JSON)
					.exchangeToMono(toResponseBody()).block();
			assertThat(body).hasEntrySatisfying("exception",
					(value) -> assertThat(value).asString().contains("IllegalStateException"));
			assertThat(body).hasEntrySatisfying("message",
					(value) -> assertThat(value).asString().contains("Epic Fail"));
		}));
	}

	@Test
	void errorEndpointIsUsedWithRestControllerEndpointOnBindingError() {
		this.runner.run(withWebTestClient((client) -> {
			Map<String, ?> body = client.post().uri("actuator/failController")
					.bodyValue(Collections.singletonMap("content", "")).accept(MediaType.APPLICATION_JSON)
					.exchangeToMono(toResponseBody()).block();
			assertThat(body).hasEntrySatisfying("exception",
					(value) -> assertThat(value).asString().contains("MethodArgumentNotValidException"));
			assertThat(body).hasEntrySatisfying("message",
					(value) -> assertThat(value).asString().contains("Validation failed"));
			assertThat(body).hasEntrySatisfying("errors", (value) -> assertThat(value).asList().isNotEmpty());
		}));
	}

	@Test
	void whenManagementServerBasePathIsConfiguredThenEndpointsAreBeneathThatPath() {
		this.runner.withPropertyValues("management.server.base-path:/manage").run(withWebTestClient((client) -> {
			String body = client.get().uri("manage/actuator/success").accept(MediaType.APPLICATION_JSON)
					.exchangeToMono((response) -> response.bodyToMono(String.class)).block();
			assertThat(body).isEqualTo("Success");
		}));
	}

	private ContextConsumer<AssertableWebApplicationContext> withWebTestClient(Consumer<WebClient> webClient) {
		return (context) -> {
			String port = context.getEnvironment().getProperty("local.management.port");
			WebClient client = WebClient.create("http://localhost:" + port);
			webClient.accept(client);
		};
	}

	private Function<ClientResponse, ? extends Mono<Map<String, ?>>> toResponseBody() {
		return ((clientResponse) -> clientResponse.bodyToMono(new ParameterizedTypeReference<Map<String, ?>>() {
		}));
	}

	@Endpoint(id = "fail")
	static class FailingEndpoint {

		@ReadOperation
		String fail() {
			throw new IllegalStateException("Epic Fail");
		}

	}

	@Endpoint(id = "success")
	static class SucceedingEndpoint {

		@ReadOperation
		String fail() {
			return "Success";
		}

	}

	@RestControllerEndpoint(id = "failController")
	static class FailingControllerEndpoint {

		@GetMapping
		String fail() {
			throw new IllegalStateException("Epic Fail");
		}

		@PostMapping(produces = "application/json")
		@ResponseBody
		String bodyValidation(@Valid @RequestBody TestBody body) {
			return body.getContent();
		}

	}

	public static class TestBody {

		@NotEmpty
		private String content;

		public String getContent() {
			return this.content;
		}

		public void setContent(String content) {
			this.content = content;
		}

	}

}
