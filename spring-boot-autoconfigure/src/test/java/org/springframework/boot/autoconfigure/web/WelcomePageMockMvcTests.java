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
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for welcome page using {@link MockMvc} and {@link SpringJUnit4ClassRunner}.
 *
 * @author Dave Syer
 */
public class WelcomePageMockMvcTests {

	private ConfigurableWebApplicationContext wac;

	private MockMvc mockMvc;

	@After
	public void close() {
		if (this.wac != null) {
			this.wac.close();
		}
	}

	@Test
	public void homePageNotFound() throws Exception {
		this.wac = (ConfigurableWebApplicationContext) new SpringApplicationBuilder(
				TestConfiguration.class).run();
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		this.mockMvc.perform(get("/")).andExpect(status().isNotFound()).andReturn();
	}

	@Test
	public void homePageCustomLocation() throws Exception {
		this.wac = (ConfigurableWebApplicationContext) new SpringApplicationBuilder(
				TestConfiguration.class)
						.properties("spring.resources.staticLocations:classpath:/custom/")
						.run();
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		this.mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();
	}

	@Test
	public void homePageCustomLocationNoTrailingSlash() throws Exception {
		this.wac = (ConfigurableWebApplicationContext) new SpringApplicationBuilder(
				TestConfiguration.class)
						.properties("spring.resources.staticLocations:classpath:/custom")
						.run();
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		this.mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn();
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, ErrorMvcAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {
	}

	@Configuration
	@MinimalWebConfiguration
	public static class TestConfiguration {

		@Bean
		public MockEmbeddedServletContainerFactory embeddedServletContainerFactory() {
			return new MockEmbeddedServletContainerFactory();
		}

		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(TestConfiguration.class, args);
		}

	}

}
