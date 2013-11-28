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

package org.springframework.boot.actuate.web;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Dave Syer
 */
public class BasicErrorControllerSpecialIntegrationTests {

	private ConfigurableWebApplicationContext wac;

	private MockMvc mockMvc;

	@After
	public void close() {
		if (this.wac != null) {
			this.wac.close();
		}
	}

	public void setup(ConfigurableWebApplicationContext context) {
		this.wac = context;
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void errorPageAvailableWithParentContext() throws Exception {
		setup((ConfigurableWebApplicationContext) new SpringApplicationBuilder(
				ParentConfiguration.class).child(ChildConfiguration.class).run());
		MvcResult response = this.mockMvc
				.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertTrue("Wrong content: " + content, content.contains("status=999"));
	}

	@Test
	public void errorPageAvailableWithMvcIncluded() throws Exception {
		setup((ConfigurableWebApplicationContext) SpringApplication
				.run(WebMvcIncludedConfiguration.class));
		MvcResult response = this.mockMvc
				.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertTrue("Wrong content: " + content, content.contains("status=999"));
	}

	@Configuration
	// TODO: fix this so it doesn't need to be excluded
	@EnableAutoConfiguration(exclude = ServerPropertiesAutoConfiguration.class)
	protected static class ParentConfiguration {

	}

	@Configuration
	@EnableAutoConfiguration
	@EnableWebMvc
	protected static class WebMvcIncludedConfiguration {
		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(WebMvcIncludedConfiguration.class, args);
		}

	}

	@Configuration
	@EnableAutoConfiguration
	protected static class ChildConfiguration {

		// For manual testing
		public static void main(String[] args) {
			new SpringApplicationBuilder(ParentConfiguration.class).child(
					ChildConfiguration.class).run(args);
		}
	}

}
