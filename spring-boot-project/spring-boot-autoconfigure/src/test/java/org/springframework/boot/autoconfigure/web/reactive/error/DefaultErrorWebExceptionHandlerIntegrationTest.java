/*
 * Copyright 2012-2017 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.validation.Valid;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests for {@link DefaultErrorWebExceptionHandler}
 *
 * @author Brian Clozel
 */
public class DefaultErrorWebExceptionHandlerIntegrationTest {

	private ConfigurableApplicationContext context;

	private WebTestClient webTestClient;

	@Rule
	public OutputCapture output = new OutputCapture();

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void jsonError() throws Exception {
		load();
		this.webTestClient.get().uri("/").exchange().expectStatus()
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody()
				.jsonPath("status").isEqualTo("500").jsonPath("error")
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.jsonPath("path").isEqualTo(("/")).jsonPath("message")
				.isEqualTo("Expected!").jsonPath("exception").doesNotExist()
				.jsonPath("trace").doesNotExist();
		this.output.expect(allOf(containsString("Failed to handle request [GET /]"),
				containsString("IllegalStateException")));
	}

	@Test
	public void notFound() throws Exception {
		load();
		this.webTestClient.get().uri("/notFound").exchange().expectStatus()
				.isEqualTo(HttpStatus.NOT_FOUND).expectBody().jsonPath("status")
				.isEqualTo("404").jsonPath("error")
				.isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase()).jsonPath("path")
				.isEqualTo(("/notFound")).jsonPath("exception").doesNotExist();
	}

	@Test
	public void htmlError() throws Exception {
		load();
		String body = this.webTestClient.get().uri("/").accept(MediaType.TEXT_HTML)
				.exchange().expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
				.expectHeader().contentType(MediaType.TEXT_HTML).expectBody(String.class)
				.returnResult().getResponseBody();
		assertThat(body).contains("status: 500").contains("message: Expected!");
		this.output.expect(allOf(containsString("Failed to handle request [GET /]"),
				containsString("IllegalStateException")));
	}

	@Test
	public void bindingResultError() throws Exception {
		load();
		this.webTestClient.post().uri("/bind").contentType(MediaType.APPLICATION_JSON)
				.syncBody("{}").exchange().expectStatus()
				.isEqualTo(HttpStatus.BAD_REQUEST).expectBody().jsonPath("status")
				.isEqualTo("400").jsonPath("error")
				.isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase()).jsonPath("path")
				.isEqualTo(("/bind")).jsonPath("exception").doesNotExist()
				.jsonPath("errors").isArray().jsonPath("message").isNotEmpty();

	}

	@Test
	public void includeStackTraceOnParam() throws Exception {
		load("--server.error.include-exception=true",
				"--server.error.include-stacktrace=on-trace-param");
		this.webTestClient.get().uri("/?trace=true").exchange().expectStatus()
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody()
				.jsonPath("status").isEqualTo("500").jsonPath("error")
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.jsonPath("exception").isEqualTo(IllegalStateException.class.getName())
				.jsonPath("trace").exists();
	}

	@Test
	public void alwaysIncludeStackTrace() throws Exception {
		load("--server.error.include-exception=true",
				"--server.error.include-stacktrace=always");
		this.webTestClient.get().uri("/?trace=false").exchange().expectStatus()
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody()
				.jsonPath("status").isEqualTo("500").jsonPath("error")
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.jsonPath("exception").isEqualTo(IllegalStateException.class.getName())
				.jsonPath("trace").exists();
	}

	@Test
	public void neverIncludeStackTrace() throws Exception {
		load("--server.error.include-exception=true",
				"--server.error.include-stacktrace=never");
		this.webTestClient.get().uri("/?trace=true").exchange().expectStatus()
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody()
				.jsonPath("status").isEqualTo("500").jsonPath("error")
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.jsonPath("exception").isEqualTo(IllegalStateException.class.getName())
				.jsonPath("trace").doesNotExist();
	}

	@Test
	public void statusException() throws Exception {
		load("--server.error.include-exception=true");
		this.webTestClient.get().uri("/badRequest").exchange().expectStatus()
				.isEqualTo(HttpStatus.BAD_REQUEST).expectBody().jsonPath("status")
				.isEqualTo("400").jsonPath("error")
				.isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase()).jsonPath("exception")
				.isEqualTo(ResponseStatusException.class.getName());
		this.output.expect(not(containsString("ResponseStatusException")));
	}

	@Test
	public void defaultErrorView() throws Exception {
		load("--spring.mustache.prefix=classpath:/unknown/");
		String body = this.webTestClient.get().uri("/").accept(MediaType.TEXT_HTML)
				.exchange().expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
				.expectHeader().contentType(MediaType.TEXT_HTML).expectBody(String.class)
				.returnResult().getResponseBody();
		assertThat(body).contains("Whitelabel Error Page")
				.contains("<div>Expected!</div>");
		this.output.expect(allOf(containsString("Failed to handle request [GET /]"),
				containsString("IllegalStateException")));
	}

	private void load(String... arguments) {
		List<String> args = new ArrayList<>();
		args.add("--server.port=0");
		if (arguments != null) {
			args.addAll(Arrays.asList(arguments));
		}
		SpringApplication application = new SpringApplication(Application.class);
		application.setWebApplicationType(WebApplicationType.REACTIVE);
		this.context = application.run(args.toArray(new String[args.size()]));
		this.webTestClient = WebTestClient.bindToApplicationContext(this.context).build();
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ ReactiveWebServerAutoConfiguration.class,
			HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class,
			ErrorWebFluxAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	private @interface MinimalWebConfiguration {

	}

	@Configuration
	@MinimalWebConfiguration
	@Import(MustacheAutoConfiguration.class)
	public static class Application {

		public static void main(String[] args) {
			SpringApplication application = new SpringApplication(Application.class);
			application.setWebApplicationType(WebApplicationType.REACTIVE);
			application.run(args);
		}

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

			@PostMapping(path = "/bind", produces = "application/json")
			@ResponseBody
			public String bodyValidation(@Valid @RequestBody DummyBody body) {
				return body.getContent();
			}
		}

	}

}
