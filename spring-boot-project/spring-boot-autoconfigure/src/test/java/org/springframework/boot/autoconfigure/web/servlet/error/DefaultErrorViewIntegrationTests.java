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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the default error view.
 *
 * @author Dave Syer
 */
@SpringBootTest
@DirtiesContext
class DefaultErrorViewIntegrationTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@BeforeEach
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	void testErrorForBrowserClient() throws Exception {
		MvcResult response = this.mockMvc.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("<html>");
		assertThat(content).contains("999");
	}

	@Test
	void testErrorWithHtmlEscape() throws Exception {
		MvcResult response = this.mockMvc
				.perform(get("/error")
						.requestAttr("javax.servlet.error.exception",
								new RuntimeException("<script>alert('Hello World')</script>"))
						.accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).contains("&lt;script&gt;");
		assertThat(content).contains("Hello World");
		assertThat(content).contains("999");
	}

	@Test
	void testErrorWithSpelEscape() throws Exception {
		String spel = "${T(" + getClass().getName() + ").injectCall()}";
		MvcResult response = this.mockMvc.perform(get("/error")
				.requestAttr("javax.servlet.error.exception", new RuntimeException(spel)).accept(MediaType.TEXT_HTML))
				.andExpect(status().is5xxServerError()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertThat(content).doesNotContain("injection");
	}

	public static String injectCall() {
		return "injection";
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ ServletWebServerFactoryAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@MinimalWebConfiguration
	public static class TestConfiguration {

		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(TestConfiguration.class, args);
		}

	}

}
