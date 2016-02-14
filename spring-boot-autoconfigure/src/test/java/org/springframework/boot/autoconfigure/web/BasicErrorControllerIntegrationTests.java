/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.BasicErrorControllerIntegrationTests.TestConfiguration;
import org.springframework.boot.autoconfigure.web.BasicErrorControllerMockMvcTests.MinimalWebConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link BasicErrorController} using a real HTTP server.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@WebAppConfiguration
@DirtiesContext
@IntegrationTest("server.port=0")
public class BasicErrorControllerIntegrationTests {

	private ConfigurableApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testErrorForMachineClient() throws Exception {
		load();
		ResponseEntity<Map> entity = new TestRestTemplate()
				.getForEntity(createUrl("?trace=true"), Map.class);
		assertErrorAttributes(entity.getBody(), "500", "Internal Server Error",
				IllegalStateException.class, "Expected!", "/");
		assertFalse("trace parameter should not be set",
				entity.getBody().containsKey("trace"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testErrorForMachineClientTraceParamStacktrace() throws Exception {
		load("--server.error.include-stacktrace=on-trace-param");
		ResponseEntity<Map> entity = new TestRestTemplate()
				.getForEntity(createUrl("?trace=true"), Map.class);
		assertErrorAttributes(entity.getBody(), "500", "Internal Server Error",
				IllegalStateException.class, "Expected!", "/");
		assertTrue("trace parameter should be set",
				entity.getBody().containsKey("trace"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testErrorForMachineClientNoStacktrace() throws Exception {
		load("--server.error.include-stacktrace=never");
		ResponseEntity<Map> entity = new TestRestTemplate()
				.getForEntity(createUrl("?trace=true"), Map.class);
		assertErrorAttributes(entity.getBody(), "500", "Internal Server Error",
				IllegalStateException.class, "Expected!", "/");
		assertFalse("trace parameter should not be set",
				entity.getBody().containsKey("trace"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testErrorForMachineClientAlwaysStacktrace() throws Exception {
		load("--server.error.include-stacktrace=always");
		ResponseEntity<Map> entity = new TestRestTemplate()
				.getForEntity(createUrl("?trace=false"), Map.class);
		assertErrorAttributes(entity.getBody(), "500", "Internal Server Error",
				IllegalStateException.class, "Expected!", "/");
		assertTrue("trace parameter should be set",
				entity.getBody().containsKey("trace"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testErrorForAnnotatedException() throws Exception {
		load();
		ResponseEntity<Map> entity = new TestRestTemplate()
				.getForEntity(createUrl("/annotated"), Map.class);
		assertErrorAttributes(entity.getBody(), "400", "Bad Request",
				TestConfiguration.Errors.ExpectedException.class, "Expected!",
				"/annotated");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testErrorForAnnotatedNoReasonException() throws Exception {
		load();
		ResponseEntity<Map> entity = new TestRestTemplate()
				.getForEntity(createUrl("/annotatedNoReason"), Map.class);
		assertErrorAttributes(entity.getBody(), "406", "Not Acceptable",
				TestConfiguration.Errors.NoReasonExpectedException.class,
				"Expected message", "/annotatedNoReason");
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testBindingExceptionForMachineClient() throws Exception {
		load();
		RequestEntity request = RequestEntity.get(URI.create(createUrl("/bind")))
				.accept(MediaType.APPLICATION_JSON).build();
		ResponseEntity<Map> entity = new TestRestTemplate().exchange(request, Map.class);
		String resp = entity.getBody().toString();
		assertThat(resp, containsString("Error count: 1"));
		assertThat(resp, containsString("errors=[{"));
		assertThat(resp, containsString("codes=["));
		assertThat(resp, containsString("org.springframework.validation.BindException"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testRequestBodyValidationForMachineClient() throws Exception {
		load();
		RequestEntity request = RequestEntity
				.post(URI.create(createUrl("/bodyValidation")))
				.contentType(MediaType.APPLICATION_JSON).body("{}");
		ResponseEntity<Map> entity = new TestRestTemplate().exchange(request, Map.class);
		String resp = entity.getBody().toString();
		assertThat(resp, containsString("Error count: 1"));
		assertThat(resp, containsString("errors=[{"));
		assertThat(resp, containsString("codes=["));
		assertThat(resp, containsString(
				"org.springframework.web.bind.MethodArgumentNotValidException"));
	}

	private void assertErrorAttributes(Map<?, ?> content, String status, String error,
			Class<?> exception, String message, String path) {
		assertEquals("Wrong status", status, content.get("status"));
		assertEquals("Wrong error", error, content.get("error"));
		assertEquals("Wrong exception", exception.getName(), content.get("exception"));
		assertEquals("Wrong message", message, content.get("message"));
		assertEquals("Wrong path", path, content.get("path"));
	}

	private String createUrl(String path) {
		int port = this.context.getEnvironment().getProperty("local.server.port",
				int.class);
		return "http://localhost:" + port + path;
	}

	private void load(String... arguments) {
		List<String> args = new ArrayList<String>();
		args.add("--server.port=0");
		if (arguments != null) {
			args.addAll(Arrays.asList(arguments));
		}
		this.context = SpringApplication.run(TestConfiguration.class,
				args.toArray(new String[args.size()]));
	}

	@Configuration
	@MinimalWebConfiguration
	public static class TestConfiguration {

		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(TestConfiguration.class, args);
		}

		@Bean
		public View error() {
			return new AbstractView() {
				@Override
				protected void renderMergedOutputModel(Map<String, Object> model,
						HttpServletRequest request, HttpServletResponse response)
								throws Exception {
					response.getWriter().write("ERROR_BEAN");
				}
			};
		}

		@RestController
		protected static class Errors {

			public String getFoo() {
				return "foo";
			}

			@RequestMapping("/")
			public String home() {
				throw new IllegalStateException("Expected!");
			}

			@RequestMapping("/annotated")
			public String annotated() {
				throw new ExpectedException();
			}

			@RequestMapping("/annotatedNoReason")
			public String annotatedNoReason() {
				throw new NoReasonExpectedException("Expected message");
			}

			@RequestMapping("/bind")
			public String bind() throws Exception {
				BindException error = new BindException(this, "test");
				error.rejectValue("foo", "bar.error");
				throw error;
			}

			@RequestMapping(path = "/bodyValidation", method = RequestMethod.POST, produces = "application/json")
			public String bodyValidation(@Valid @RequestBody DummyBody body) {
				return body.content;
			}

			@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Expected!")
			@SuppressWarnings("serial")
			private static class ExpectedException extends RuntimeException {

			}

			@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
			@SuppressWarnings("serial")
			private static class NoReasonExpectedException extends RuntimeException {

				NoReasonExpectedException(String message) {
					super(message);
				}

			}

			static class DummyBody {

				@NotNull
				private String content;

				public String getContent() {
					return this.content;
				}

				public void setContent(String content) {
					this.content = content;
				}

			}

		}

	}

}
