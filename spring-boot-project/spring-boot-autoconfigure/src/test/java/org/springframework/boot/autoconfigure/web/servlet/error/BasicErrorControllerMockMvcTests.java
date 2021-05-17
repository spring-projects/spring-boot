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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link BasicErrorController} using {@link MockMvc} and
 * {@link SpringBootTest @SpringBootTest}.
 *
 * @author Dave Syer
 * @author Scott Frederick
 */
@SpringBootTest(properties = { "server.error.include-message=always" })
@DirtiesContext
class BasicErrorControllerMockMvcTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@BeforeEach
	void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	void testDirectAccessForMachineClient() throws Exception {
		MvcResult response = this.mockMvc.perform(get("/error")).andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("999");
	}

	@Test
	void testErrorWithNotFoundResponseStatus() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/bang")).andExpect(status().isNotFound()).andReturn();
		MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error")).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("Expected!");
	}

	@Test
	void testErrorWithNoContentResponseStatus() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/noContent").accept("some/thing"))
				.andExpect(status().isNoContent()).andReturn();
		MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error"))
				.andExpect(status().isNoContent()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).isEmpty();
	}

	@Test
	void testBindingExceptionForMachineClient() throws Exception {
		// In a real server the response is carried over into the error dispatcher, but
		// in the mock a new one is created so we have to assert the status at this
		// intermediate point
		MvcResult result = this.mockMvc.perform(get("/bind")).andExpect(status().is4xxClientError()).andReturn();
		MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error")).andReturn();
		// And the rendered status code is always wrong (but would be 400 in a real
		// system)
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("Validation failed");
	}

	@Test
	void testDirectAccessForBrowserClient() throws Exception {
		MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("ERROR_BEAN");
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
	static class TestConfiguration {

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

			@RequestMapping("/")
			String home() {
				throw new IllegalStateException("Expected!");
			}

			@RequestMapping("/bang")
			String bang() {
				throw new NotFoundException("Expected!");
			}

			@RequestMapping("/bind")
			String bind() throws Exception {
				BindException error = new BindException(this, "test");
				error.rejectValue("foo", "bar.error");
				throw error;
			}

			@RequestMapping("/noContent")
			void noContent() {
				throw new NoContentException("Expected!");
			}

			public String getFoo() {
				return "foo";
			}

		}

	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	static class NotFoundException extends RuntimeException {

		NotFoundException(String string) {
			super(string);
		}

	}

	@ResponseStatus(HttpStatus.NO_CONTENT)
	private static class NoContentException extends RuntimeException {

		NoContentException(String string) {
			super(string);
		}

	}

	private class ErrorDispatcher implements RequestBuilder {

		private final MvcResult result;

		private final String path;

		ErrorDispatcher(MvcResult result, String path) {
			this.result = result;
			this.path = path;
		}

		@Override
		public MockHttpServletRequest buildRequest(ServletContext servletContext) {
			MockHttpServletRequest request = this.result.getRequest();
			request.setDispatcherType(DispatcherType.ERROR);
			request.setRequestURI(this.path);
			request.setAttribute("javax.servlet.error.status_code", this.result.getResponse().getStatus());
			return request;
		}

	}

}
