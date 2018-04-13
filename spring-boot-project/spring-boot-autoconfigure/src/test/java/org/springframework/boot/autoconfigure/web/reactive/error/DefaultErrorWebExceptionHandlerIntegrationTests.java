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

package org.springframework.boot.autoconfigure.web.reactive.error;

import javax.validation.Valid;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests for {@link DefaultErrorWebExceptionHandler}
 *
 * @author Brian Clozel
 */
public class DefaultErrorWebExceptionHandlerIntegrationTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					ReactiveWebServerFactoryAutoConfiguration.class,
					HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class,
					ErrorWebFluxAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class,
					MustacheAutoConfiguration.class))
			.withPropertyValues("spring.main.web-application-type=reactive",
					"server.port=0")
			.withUserConfiguration(Application.class);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void jsonError() {
		this.contextRunner.run((context) -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context)
					.build();
			client.get().uri("/").exchange().expectStatus()
					.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody()
					.jsonPath("status").isEqualTo("500").jsonPath("error")
					.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
					.jsonPath("path").isEqualTo(("/")).jsonPath("message")
					.isEqualTo("Expected!").jsonPath("exception").doesNotExist()
					.jsonPath("trace").doesNotExist();
			this.output.expect(allOf(containsString("Failed to handle request [GET /]"),
					containsString("IllegalStateException")));
		});
	}

	@Test
	public void notFound() {
		this.contextRunner.run((context) -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context)
					.build();
			client.get().uri("/notFound").exchange().expectStatus().isNotFound()
					.expectBody().jsonPath("status").isEqualTo("404").jsonPath("error")
					.isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase()).jsonPath("path")
					.isEqualTo(("/notFound")).jsonPath("exception").doesNotExist();
		});
	}

	@Test
	public void htmlError() {
		this.contextRunner.run((context) -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context)
					.build();
			String body = client.get().uri("/").accept(MediaType.TEXT_HTML).exchange()
					.expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
					.expectHeader().contentType(MediaType.TEXT_HTML)
					.expectBody(String.class).returnResult().getResponseBody();
			assertThat(body).contains("status: 500").contains("message: Expected!");
			this.output.expect(allOf(containsString("Failed to handle request [GET /]"),
					containsString("IllegalStateException")));
		});
	}

	@Test
	public void bindingResultError() {
		this.contextRunner.run((context) -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context)
					.build();
			client.post().uri("/bind").contentType(MediaType.APPLICATION_JSON)
					.syncBody("{}").exchange().expectStatus().isBadRequest().expectBody()
					.jsonPath("status").isEqualTo("400").jsonPath("error")
					.isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase()).jsonPath("path")
					.isEqualTo(("/bind")).jsonPath("exception").doesNotExist()
					.jsonPath("errors").isArray().jsonPath("message").isNotEmpty();
		});
		this.output.expect(allOf(containsString("Failed to handle request [POST /bind]"),
				containsString("Validation failed for argument"),
				containsString("Field error in object 'dummyBody' on field 'content'")));
	}

	@Test
	public void includeStackTraceOnParam() {
		this.contextRunner
				.withPropertyValues("server.error.include-exception=true",
						"server.error.include-stacktrace=on-trace-param")
				.run((context) -> {
					WebTestClient client = WebTestClient.bindToApplicationContext(context)
							.build();
					client.get().uri("/?trace=true").exchange().expectStatus()
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody()
							.jsonPath("status").isEqualTo("500").jsonPath("error")
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
							.jsonPath("exception")
							.isEqualTo(IllegalStateException.class.getName())
							.jsonPath("trace").exists();
				});
	}

	@Test
	public void alwaysIncludeStackTrace() throws Exception {
		this.contextRunner.withPropertyValues("server.error.include-exception=true",
				"server.error.include-stacktrace=always").run((context) -> {
					WebTestClient client = WebTestClient.bindToApplicationContext(context)
							.build();
					client.get().uri("/?trace=false").exchange().expectStatus()
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody()
							.jsonPath("status").isEqualTo("500").jsonPath("error")
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
							.jsonPath("exception")
							.isEqualTo(IllegalStateException.class.getName())
							.jsonPath("trace").exists();
				});
	}

	@Test
	public void neverIncludeStackTrace() {
		this.contextRunner.withPropertyValues("server.error.include-exception=true",
				"server.error.include-stacktrace=never").run((context) -> {
					WebTestClient client = WebTestClient.bindToApplicationContext(context)
							.build();
					client.get().uri("/?trace=true").exchange().expectStatus()
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody()
							.jsonPath("status").isEqualTo("500").jsonPath("error")
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
							.jsonPath("exception")
							.isEqualTo(IllegalStateException.class.getName())
							.jsonPath("trace").doesNotExist();

				});
	}

	@Test
	public void statusException() {
		this.contextRunner.withPropertyValues("server.error.include-exception=true")
				.run((context) -> {
					WebTestClient client = WebTestClient.bindToApplicationContext(context)
							.build();
					client.get().uri("/badRequest").exchange().expectStatus()
							.isBadRequest().expectBody().jsonPath("status")
							.isEqualTo("400").jsonPath("error")
							.isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase())
							.jsonPath("exception")
							.isEqualTo(ResponseStatusException.class.getName());
					this.output.expect(not(containsString("ResponseStatusException")));
				});
	}

	@Test
	public void defaultErrorView() {
		this.contextRunner
				.withPropertyValues("spring.mustache.prefix=classpath:/unknown/")
				.run((context) -> {
					WebTestClient client = WebTestClient.bindToApplicationContext(context)
							.build();
					String body = client.get().uri("/").accept(MediaType.TEXT_HTML)
							.exchange().expectStatus()
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectHeader()
							.contentType(MediaType.TEXT_HTML).expectBody(String.class)
							.returnResult().getResponseBody();
					assertThat(body).contains("Whitelabel Error Page")
							.contains("<div>Expected!</div>");
					this.output.expect(
							allOf(containsString("Failed to handle request [GET /]"),
									containsString("IllegalStateException")));
				});
	}

	@Test
	public void escapeHtmlInDefaultErrorView() {
		this.contextRunner
				.withPropertyValues("spring.mustache.prefix=classpath:/unknown/")
				.run((context) -> {
					WebTestClient client = WebTestClient.bindToApplicationContext(context)
							.build();
					String body = client.get().uri("/html").accept(MediaType.TEXT_HTML)
							.exchange().expectStatus()
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectHeader()
							.contentType(MediaType.TEXT_HTML).expectBody(String.class)
							.returnResult().getResponseBody();
					assertThat(body).contains("Whitelabel Error Page")
							.doesNotContain("<script>").contains("&lt;script&gt;");
					this.output.expect(
							allOf(containsString("Failed to handle request [GET /html]"),
									containsString("IllegalStateException")));
				});
	}

	@Test
	public void testExceptionWithNullMessage() {
		this.contextRunner
				.withPropertyValues("spring.mustache.prefix=classpath:/unknown/")
				.run((context) -> {
					WebTestClient client = WebTestClient.bindToApplicationContext(context)
							.build();
					String body = client.get().uri("/notfound")
							.accept(MediaType.TEXT_HTML).exchange().expectStatus()
							.isNotFound().expectHeader().contentType(MediaType.TEXT_HTML)
							.expectBody(String.class).returnResult().getResponseBody();
					assertThat(body).contains("Whitelabel Error Page")
							.contains("type=Not Found, status=404");
				});
	}

	@Test
	public void responseCommitted() {
		this.contextRunner.run((context) -> {
			WebTestClient client = WebTestClient.bindToApplicationContext(context)
					.build();
			this.thrown.expectCause(instanceOf(IllegalStateException.class));
			this.thrown.expectMessage("already committed!");
			client.get().uri("/commit").exchange().expectStatus();
		});
	}

	@Test
	public void whitelabelDisabled() {
		this.contextRunner.withPropertyValues("server.error.whitelabel.enabled=false",
				"spring.mustache.prefix=classpath:/unknown/").run((context) -> {
					WebTestClient client = WebTestClient.bindToApplicationContext(context)
							.build();
					client.get().uri("/notfound").accept(MediaType.TEXT_HTML).exchange()
							.expectStatus().isNotFound().expectBody().isEmpty();
				});
	}

	@Configuration
	public static class Application {

		@RestController
		protected static class ErrorController {

			@GetMapping("/")
			public String home() {
				throw new IllegalStateException("Expected!");
			}

			@GetMapping("/badRequest")
			public Mono<String> badRequest() {
				return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
			}

			@GetMapping("/commit")
			public Mono<Void> commit(ServerWebExchange exchange) {
				return exchange.getResponse().writeWith(Mono.empty()).then(
						Mono.error(new IllegalStateException("already committed!")));
			}

			@GetMapping("/html")
			public String htmlEscape() {
				throw new IllegalStateException("<script>");
			}

			@PostMapping(path = "/bind", produces = "application/json")
			@ResponseBody
			public String bodyValidation(@Valid @RequestBody DummyBody body) {
				return body.getContent();
			}
		}

	}

}
