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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DefaultErrorViewIntegrationTests.TestConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

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
public class DefaultErrorViewIntegrationTests {

	@Autowired
	private WebApplicationContext wac;

	private MockMvc mockMvc;

	@Before
	public void setup() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
	}

	@Test
	public void testErrorForBrowserClient() throws Exception {
		MvcResult response = this.mockMvc
				.perform(get("/error").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andReturn();
		String content = response.getResponse().getContentAsString();
		assertTrue("Wrong content: " + content, content.contains("<html>"));
		assertTrue("Wrong content: " + content, content.contains("999"));
	}

	@Configuration
	@EnableAutoConfiguration
	public static class TestConfiguration {

		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(TestConfiguration.class, args);
		}

	}

}
