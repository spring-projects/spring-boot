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

package org.springframework.boot.autoconfigure.web.reactive.error;

import java.nio.charset.StandardCharsets;

import javax.validation.Valid;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for {@link DefaultErrorWebExceptionHandler}
 *
 * @author Brian Clozel
 */
@ExtendWith(OutputCaptureExtension.class)
class DefaultErrorWebExceptionHandlerIntegrationTests {

	private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

	private final LogIdFilter logIdFilter = new LogIdFilter();

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveWebServerFactoryAutoConfiguration.class,
					HttpHandlerAutoConfiguration.class, WebFluxAutoConfiguration.class,
					ErrorWebFluxAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
					MustacheAutoConfiguration.class))
			.withPropertyValues("spring.main.web-application-type=reactive", "server.port=0")
			.withUserConfiguration(Application.class);

	@Test
	void jsonError(CapturedOutput capturedOutput) {
		this.contextRunner.run((context) -> {
			WebTestClient client = getWebClient(context);
			client.get().uri("/").exchange().expectStatus().isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody()
					.jsonPath("status").isEqualTo("500").jsonPath("error")
					.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()).jsonPath("path").isEqualTo(("/"))
					.jsonPath("message").isEqualTo("Expected!").jsonPath("exception").doesNotExist().jsonPath("trace")
					.doesNotExist().jsonPath("requestId").isEqualTo(this.logIdFilter.getLogId());
			assertThat(capturedOutput).contains("500 Server Error for HTTP GET \"/\"")
					.contains("java.lang.IllegalStateException: Expected!");
		});
	}

	@Test
	void notFound() {
		this.contextRunner.run((context) -> {
			WebTestClient client = getWebClient(context);
			client.get().uri("/notFound").exchange().expectStatus().isNotFound().expectBody().jsonPath("status")
					.isEqualTo("404").jsonPath("error").isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase())
					.jsonPath("path").isEqualTo(("/notFound")).jsonPath("exception").doesNotExist()
					.jsonPath("requestId").isEqualTo(this.logIdFilter.getLogId());
		});
	}

	@Test
	void htmlError() {
		this.contextRunner.run((context) -> {
			WebTestClient client = getWebClient(context);
			String body = client.get().uri("/").accept(MediaType.TEXT_HTML).exchange().expectStatus()
					.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectHeader().contentType(TEXT_HTML_UTF8)
					.expectBody(String.class).returnResult().getResponseBody();
			assertThat(body).contains("status: 500").contains("message: Expected!");
		});
	}

	@Test
	void bindingResultError() {
		this.contextRunner.run((context) -> {
			WebTestClient client = getWebClient(context);
			client.post().uri("/bind").contentType(MediaType.APPLICATION_JSON).syncBody("{}").exchange().expectStatus()
					.isBadRequest().expectBody().jsonPath("status").isEqualTo("400").jsonPath("error")
					.isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase()).jsonPath("path").isEqualTo(("/bind"))
					.jsonPath("exception").doesNotExist().jsonPath("errors").isArray().jsonPath("message").isNotEmpty()
					.jsonPath("requestId").isEqualTo(this.logIdFilter.getLogId());
		});
	}

	@Test
	void includeStackTraceOnParam() {
		this.contextRunner.withPropertyValues("server.error.include-exception=true",
				"server.error.include-stacktrace=on-trace-param").run((context) -> {
					WebTestClient client = getWebClient(context);
					client.get().uri("/?trace=true").exchange().expectStatus()
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody().jsonPath("status")
							.isEqualTo("500").jsonPath("error")
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()).jsonPath("exception")
							.isEqualTo(IllegalStateException.class.getName()).jsonPath("trace").exists()
							.jsonPath("requestId").isEqualTo(this.logIdFilter.getLogId());
				});
	}

	@Test
	void alwaysIncludeStackTrace() {
		this.contextRunner
				.withPropertyValues("server.error.include-exception=true", "server.error.include-stacktrace=always")
				.run((context) -> {
					WebTestClient client = getWebClient(context);
					client.get().uri("/?trace=false").exchange().expectStatus()
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody().jsonPath("status")
							.isEqualTo("500").jsonPath("error")
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()).jsonPath("exception")
							.isEqualTo(IllegalStateException.class.getName()).jsonPath("trace").exists()
							.jsonPath("requestId").isEqualTo(this.logIdFilter.getLogId());
				});
	}

	@Test
	void neverIncludeStackTrace() {
		this.contextRunner
				.withPropertyValues("server.error.include-exception=true", "server.error.include-stacktrace=never")
				.run((context) -> {
					WebTestClient client = getWebClient(context);
					client.get().uri("/?trace=true").exchange().expectStatus()
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectBody().jsonPath("status")
							.isEqualTo("500").jsonPath("error")
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase()).jsonPath("exception")
							.isEqualTo(IllegalStateException.class.getName()).jsonPath("trace").doesNotExist()
							.jsonPath("requestId").isEqualTo(this.logIdFilter.getLogId());
				});
	}

	@Test
	void statusException() {
		this.contextRunner.withPropertyValues("server.error.include-exception=true").run((context) -> {
			WebTestClient client = getWebClient(context);
			client.get().uri("/badRequest").exchange().expectStatus().isBadRequest().expectBody().jsonPath("status")
					.isEqualTo("400").jsonPath("error").isEqualTo(HttpStatus.BAD_REQUEST.getReasonPhrase())
					.jsonPath("exception").isEqualTo(ResponseStatusException.class.getName()).jsonPath("requestId")
					.isEqualTo(this.logIdFilter.getLogId());
		});
	}

	@Test
	void defaultErrorView() {
		this.contextRunner.withPropertyValues("spring.mustache.prefix=classpath:/unknown/",
				"server.error.include-stacktrace=always").run((context) -> {
					WebTestClient client = getWebClient(context);
					String body = client.get().uri("/").accept(MediaType.TEXT_HTML).exchange().expectStatus()
							.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectHeader().contentType(TEXT_HTML_UTF8)
							.expectBody(String.class).returnResult().getResponseBody();
					assertThat(body).contains("Whitelabel Error Page").contains(this.logIdFilter.getLogId())
							.contains("<div>Expected!</div>")
							.contains("<div style='white-space:pre-wrap;'>java.lang.IllegalStateException");
				});
	}

	@Test
	void escapeHtmlInDefaultErrorView() {
		this.contextRunner.withPropertyValues("spring.mustache.prefix=classpath:/unknown/").run((context) -> {
			WebTestClient client = getWebClient(context);
			String body = client.get().uri("/html").accept(MediaType.TEXT_HTML).exchange().expectStatus()
					.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR).expectHeader().contentType(TEXT_HTML_UTF8)
					.expectBody(String.class).returnResult().getResponseBody();
			assertThat(body).contains("Whitelabel Error Page").contains(this.logIdFilter.getLogId())
					.doesNotContain("<script>").contains("&lt;script&gt;");
		});
	}

	@Test
	void testExceptionWithNullMessage() {
		this.contextRunner.withPropertyValues("spring.mustache.prefix=classpath:/unknown/").run((context) -> {
			WebTestClient client = getWebClient(context);
			String body = client.get().uri("/notfound").accept(MediaType.TEXT_HTML).exchange().expectStatus()
					.isNotFound().expectHeader().contentType(TEXT_HTML_UTF8).expectBody(String.class).returnResult()
					.getResponseBody();
			assertThat(body).contains("Whitelabel Error Page").contains(this.logIdFilter.getLogId())
					.contains("type=Not Found, status=404");
		});
	}

	@Test
	void responseCommitted() {
		this.contextRunner.run((context) -> {
			WebTestClient client = getWebClient(context);
			assertThatExceptionOfType(RuntimeException.class)
					.isThrownBy(() -> client.get().uri("/commit").exchange().expectStatus())
					.withCauseInstanceOf(IllegalStateException.class).withMessageContaining("already committed!");
		});
	}

	@Test
	void whitelabelDisabled() {
		this.contextRunner.withPropertyValues("server.error.whitelabel.enabled=false",
				"spring.mustache.prefix=classpath:/unknown/").run((context) -> {
					WebTestClient client = getWebClient(context);
					client.get().uri("/notfound").accept(MediaType.TEXT_HTML).exchange().expectStatus().isNotFound()
							.expectBody().isEmpty();
				});
	}

	@Test
	void exactStatusTemplateErrorPage() {
		this.contextRunner.withPropertyValues("server.error.whitelabel.enabled=false",
				"spring.mustache.prefix=" + getErrorTemplatesLocation()).run((context) -> {
					WebTestClient client = getWebClient(context);
					String body = client.get().uri("/notfound").accept(MediaType.TEXT_HTML).exchange().expectStatus()
							.isNotFound().expectBody(String.class).returnResult().getResponseBody();
					assertThat(body).contains("404 page");
				});
	}

	@Test
	void seriesStatusTemplateErrorPage() {
		this.contextRunner.withPropertyValues("server.error.whitelabel.enabled=false",
				"spring.mustache.prefix=" + getErrorTemplatesLocation()).run((context) -> {
					WebTestClient client = getWebClient(context);
					String body = client.get().uri("/badRequest").accept(MediaType.TEXT_HTML).exchange().expectStatus()
							.isBadRequest().expectBody(String.class).returnResult().getResponseBody();
					assertThat(body).contains("4xx page");
				});
	}

	@Test
	void invalidAcceptMediaType() {
		this.contextRunner.run((context) -> {
			WebTestClient client = getWebClient(context);
			client.get().uri("/notfound").header("Accept", "v=3.0").exchange().expectStatus()
					.isEqualTo(HttpStatus.NOT_FOUND);
		});
	}

	private String getErrorTemplatesLocation() {
		String packageName = getClass().getPackage().getName();
		return "classpath:/" + packageName.replace('.', '/') + "/templates/";
	}

	private WebTestClient getWebClient(AssertableReactiveWebApplicationContext context) {
		return WebTestClient.bindToApplicationContext(context).webFilter(this.logIdFilter).build();
	}

	private static final class LogIdFilter implements WebFilter {

		private String logId;

		@Override
		public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
			this.logId = exchange.getRequest().getId();
			return chain.filter(exchange);
		}

		String getLogId() {
			return this.logId;
		}

	}

	@Configuration(proxyBeanMethods = false)
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
				return exchange.getResponse().setComplete()
						.then(Mono.error(new IllegalStateException("already committed!")));
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
