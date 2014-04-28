/*
 * Copyright 2012-2013 the original author or authors.
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.BasicErrorControllerIntegrationTests.TestConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Dave Syer
 */
@SpringApplicationConfiguration(classes = TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@DirtiesContext
public class BasicErrorControllerIntegrationTests {

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
	public void testErrorForMachineClient() throws Exception {
		MvcResult result = this.mockMvc.perform(get("/"))
				.andExpect(status().is5xxServerError()).andReturn();
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

	@Configuration
	@EnableAutoConfiguration(exclude = { SecurityAutoConfiguration.class,
			DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class })
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

			@RequestMapping("/bind")
			public String bind() throws Exception {
				BindException error = new BindException(this, "test");
				error.rejectValue("foo", "bar.error");
				throw error;
			}

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
