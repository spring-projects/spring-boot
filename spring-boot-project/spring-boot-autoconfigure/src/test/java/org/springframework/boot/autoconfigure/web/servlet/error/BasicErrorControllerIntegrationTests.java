/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BasicErrorController} using a real HTTP server.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class BasicErrorControllerIntegrationTests {

	private ConfigurableApplicationContext context;

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	void testErrorForMachineClient() {
		load();
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("?trace=true"), Map.class);
		assertErrorAttributes(entity.getBody(), "500", "Internal Server Error", null, "Expected!", "/");
		assertThat(entity.getBody().containsKey("trace")).isFalse();
	}

	@Test
	void testErrorForMachineClientTraceParamTrue() {
		errorForMachineClientOnTraceParam("?trace=true", true);
	}

	@Test
	void testErrorForMachineClientTraceParamFalse() {
		errorForMachineClientOnTraceParam("?trace=false", false);
	}

	@Test
	void testErrorForMachineClientTraceParamAbsent() {
		errorForMachineClientOnTraceParam("", false);
	}

	@SuppressWarnings("rawtypes")
	private void errorForMachineClientOnTraceParam(String path, boolean expectedTrace) {
		load("--server.error.include-exception=true", "--server.error.include-stacktrace=on-trace-param");
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl(path), Map.class);
		assertErrorAttributes(entity.getBody(), "500", "Internal Server Error", IllegalStateException.class,
				"Expected!", "/");
		assertThat(entity.getBody().containsKey("trace")).isEqualTo(expectedTrace);
	}

	@Test
	@SuppressWarnings("rawtypes")
	void testErrorForMachineClientNoStacktrace() {
		load("--server.error.include-stacktrace=never");
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("?trace=true"), Map.class);
		assertErrorAttributes(entity.getBody(), "500", "Internal Server Error", null, "Expected!", "/");
		assertThat(entity.getBody().containsKey("trace")).isFalse();
	}

	@Test
	@SuppressWarnings("rawtypes")
	void testErrorForMachineClientAlwaysStacktrace() {
		load("--server.error.include-stacktrace=always");
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("?trace=false"), Map.class);
		assertErrorAttributes(entity.getBody(), "500", "Internal Server Error", null, "Expected!", "/");
		assertThat(entity.getBody().containsKey("trace")).isTrue();
	}

	@Test
	@SuppressWarnings("rawtypes")
	void testErrorForAnnotatedException() {
		load("--server.error.include-exception=true");
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/annotated"), Map.class);
		assertErrorAttributes(entity.getBody(), "400", "Bad Request", TestConfiguration.Errors.ExpectedException.class,
				"Expected!", "/annotated");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void testErrorForAnnotatedNoReasonException() {
		load("--server.error.include-exception=true");
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(createUrl("/annotatedNoReason"), Map.class);
		assertErrorAttributes(entity.getBody(), "406", "Not Acceptable",
				TestConfiguration.Errors.NoReasonExpectedException.class, "Expected message", "/annotatedNoReason");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void testBindingExceptionForMachineClient() {
		load("--server.error.include-exception=true");
		RequestEntity request = RequestEntity.get(URI.create(createUrl("/bind"))).accept(MediaType.APPLICATION_JSON)
				.build();
		ResponseEntity<Map> entity = new TestRestTemplate().exchange(request, Map.class);
		String resp = entity.getBody().toString();
		assertThat(resp).contains("Error count: 1");
		assertThat(resp).contains("errors=[{");
		assertThat(resp).contains("codes=[");
		assertThat(resp).contains("org.springframework.validation.BindException");
	}

	@Test
	@SuppressWarnings("rawtypes")
	void testRequestBodyValidationForMachineClient() {
		load("--server.error.include-exception=true");
		RequestEntity request = RequestEntity.post(URI.create(createUrl("/bodyValidation")))
				.accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).body("{}");
		ResponseEntity<Map> entity = new TestRestTemplate().exchange(request, Map.class);
		String resp = entity.getBody().toString();
		assertThat(resp).contains("Error count: 1");
		assertThat(resp).contains("errors=[{");
		assertThat(resp).contains("codes=[");
		assertThat(resp).contains(MethodArgumentNotValidException.class.getName());
	}

	@Test
	@SuppressWarnings("rawtypes")
	void testNoExceptionByDefaultForMachineClient() {
		load();
		RequestEntity request = RequestEntity.get(URI.create(createUrl("/bind"))).accept(MediaType.APPLICATION_JSON)
				.build();
		ResponseEntity<Map> entity = new TestRestTemplate().exchange(request, Map.class);
		String resp = entity.getBody().toString();
		assertThat(resp).doesNotContain("org.springframework.validation.BindException");
	}

	@Test
	void testConventionTemplateMapping() {
		load();
		RequestEntity<?> request = RequestEntity.get(URI.create(createUrl("/noStorage"))).accept(MediaType.TEXT_HTML)
				.build();
		ResponseEntity<String> entity = new TestRestTemplate().exchange(request, String.class);
		String resp = entity.getBody();
		assertThat(resp).contains("We are out of storage");
	}

	@Test
	void testIncompatibleMediaType() {
		load();
		RequestEntity<?> request = RequestEntity.get(URI.create(createUrl("/incompatibleType")))
				.accept(MediaType.TEXT_PLAIN).build();
		ResponseEntity<String> entity = new TestRestTemplate().exchange(request, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(entity.getHeaders().getContentType()).isNull();
		assertThat(entity.getBody()).isNull();
	}

	private void assertErrorAttributes(Map<?, ?> content, String status, String error, Class<?> exception,
			String message, String path) {
		assertThat(content.get("status")).as("Wrong status").isEqualTo(status);
		assertThat(content.get("error")).as("Wrong error").isEqualTo(error);
		if (exception != null) {
			assertThat(content.get("exception")).as("Wrong exception").isEqualTo(exception.getName());
		}
		else {
			assertThat(content.containsKey("exception")).as("Exception attribute should not be set").isFalse();
		}
		assertThat(content.get("message")).as("Wrong message").isEqualTo(message);
		assertThat(content.get("path")).as("Wrong path").isEqualTo(path);
	}

	private String createUrl(String path) {
		int port = this.context.getEnvironment().getProperty("local.server.port", int.class);
		return "http://localhost:" + port + path;
	}

	private void load(String... arguments) {
		List<String> args = new ArrayList<>();
		args.add("--server.port=0");
		if (arguments != null) {
			args.addAll(Arrays.asList(arguments));
		}
		this.context = SpringApplication.run(TestConfiguration.class, StringUtils.toStringArray(args));
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ImportAutoConfiguration({ ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	private @interface MinimalWebConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	@ImportAutoConfiguration(FreeMarkerAutoConfiguration.class)
	public static class TestConfiguration {

		// For manual testing
		static void main(String[] args) {
			SpringApplication.run(TestConfiguration.class, args);
		}

		@Bean
		View error() {
			return new AbstractView() {
				@Override
				protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
						HttpServletResponse response) throws Exception {
					response.getWriter().write("ERROR_BEAN");
				}
			};
		}

		@RestController
		public static class Errors {

			public String getFoo() {
				return "foo";
			}

			@RequestMapping("/")
			String home() {
				throw new IllegalStateException("Expected!");
			}

			@RequestMapping("/annotated")
			String annotated() {
				throw new ExpectedException();
			}

			@RequestMapping("/annotatedNoReason")
			String annotatedNoReason() {
				throw new NoReasonExpectedException("Expected message");
			}

			@RequestMapping("/bind")
			String bind() throws Exception {
				BindException error = new BindException(this, "test");
				error.rejectValue("foo", "bar.error");
				throw error;
			}

			@PostMapping(path = "/bodyValidation", produces = "application/json")
			String bodyValidation(@Valid @RequestBody DummyBody body) {
				return body.content;
			}

			@RequestMapping(path = "/noStorage")
			String noStorage() {
				throw new InsufficientStorageException();
			}

			@RequestMapping(path = "/incompatibleType", produces = "text/plain")
			String incompatibleType() {
				throw new ExpectedException();
			}

			@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Expected!")
			@SuppressWarnings("serial")
			static class ExpectedException extends RuntimeException {

			}

			@ResponseStatus(HttpStatus.INSUFFICIENT_STORAGE)
			static class InsufficientStorageException extends RuntimeException {

			}

			@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
			@SuppressWarnings("serial")
			static class NoReasonExpectedException extends RuntimeException {

				NoReasonExpectedException(String message) {
					super(message);
				}

			}

			static class DummyBody {

				@NotNull
				private String content;

				String getContent() {
					return this.content;
				}

				void setContent(String content) {
					this.content = content;
				}

			}

		}

	}

}
