/*
 * Copyright 2012-2014 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.BasicErrorControllerMockMvcTests.TestConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
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

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link BasicErrorController} using {@link MockMvc} and
 * {@link SpringJUnit4ClassRunner}.
 *
 * @author Dave Syer
 */
@SpringApplicationConfiguration(classes = TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@DirtiesContext
public class BasicErrorControllerMockMvcTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void testDirectAccessForMachineClient() throws Exception {
		MvcResult response = this.mockMvc.perform(get("/error"))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertTrue("Wrong content: " + content, content.contains("999"));
	}

	@Test
	public void testErrorWithResponseStatus() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/bang"))
				.andExpect(status().isNotFound()).andReturn();
		MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error"))
				.andReturn();
		String content = response.getResponse().getContentAsString();
		assertTrue("Wrong content: " + content, content.contains("Expected!"));
	}

	@Test
	public void testBindingExceptionForMachineClient() throws Exception {
		// In a real container the response is carried over into the error dispatcher, but
		// in the mock a new one is created so we have to assert the status at this
		// intermediate point
		MvcResult result = this.mockMvc.perform(get("/bind"))
				.andExpect(status().is4xxClientError()).andReturn();
		MvcResult response = this.mockMvc.perform(new ErrorDispatcher(result, "/error"))
				.andReturn();
		// And the rendered status code is always wrong (but would be 400 in a real
		// system)
		String content = response.getResponse().getContentAsString();
		assertTrue("Wrong content: " + content, content.contains("Error count: 1"));
	}

	@Test
	public void testDirectAccessForBrowserClient() throws Exception {
		MvcResult response = this.mockMvc
				.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertTrue("Wrong content: " + content, content.contains("ERROR_BEAN"));
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ EmbeddedServletContainerAutoConfiguration.EmbeddedTomcat.class,
			EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static @interface MinimalWebConfiguration {
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

			@RequestMapping("/bang")
			public String bang() {
				throw new NotFoundException("Expected!");
			}

			@RequestMapping("/bind")
			public String bind() throws Exception {
				BindException error = new BindException(this, "test");
				error.rejectValue("foo", "bar.error");
				throw error;
			}

		}

	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	private static class NotFoundException extends RuntimeException {

		public NotFoundException(String string) {
			super(string);
		}

	}

	private class ErrorDispatcher implements RequestBuilder {

		private MvcResult result;
		private String path;

		public ErrorDispatcher(MvcResult result, String path) {
			this.result = result;
			this.path = path;
		}

		@Override
		public MockHttpServletRequest buildRequest(ServletContext servletContext) {
			MockHttpServletRequest request = this.result.getRequest();
			request.setDispatcherType(DispatcherType.ERROR);
			request.setRequestURI(this.path);
			return request;
		}
	}

}
