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

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.ApplicationContextTestUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link BasicErrorController} using {@link MockMvc} but not
 * {@link SpringJUnit4ClassRunner}.
 * 
 * @author Dave Syer
 */
public class BasicErrorControllerDirectMockMvcTests {

	private ConfigurableWebApplicationContext wac;

	private MockMvc mockMvc;

	@After
	public void close() {
		ApplicationContextTestUtils.closeAll(this.wac);
	}

	public void setup(ConfigurableWebApplicationContext context) {
		this.wac = context;
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void errorPageAvailableWithParentContext() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplicationBuilder(
				ParentConfiguration.class).child(ChildConfiguration.class).run(
				"--server.port=0"));
		MvcResult response = this.mockMvc
				.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertTrue("Wrong content: " + content, content.contains("status=999"));
	}

	@Test
	public void errorPageAvailableWithMvcIncluded() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplication(
				WebMvcIncludedConfiguration.class).run("--server.port=0"));
		MvcResult response = this.mockMvc
				.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertTrue("Wrong content: " + content, content.contains("status=999"));
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static @interface MinimalWebConfiguration {
	}

	@Configuration
	@MinimalWebConfiguration
	protected static class ParentConfiguration {

	}

	@Configuration
	@MinimalWebConfiguration
	@EnableWebMvc
	protected static class WebMvcIncludedConfiguration {
		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(WebMvcIncludedConfiguration.class, args);
		}

	}

	@Configuration
	@MinimalWebConfiguration
	protected static class VanillaConfiguration {
		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(VanillaConfiguration.class, args);
		}

	}

	@Configuration
	@MinimalWebConfiguration
	protected static class ChildConfiguration {
		// For manual testing
		public static void main(String[] args) {
			new SpringApplicationBuilder(ParentConfiguration.class).child(
					ChildConfiguration.class).run(args);
		}
	}

}
