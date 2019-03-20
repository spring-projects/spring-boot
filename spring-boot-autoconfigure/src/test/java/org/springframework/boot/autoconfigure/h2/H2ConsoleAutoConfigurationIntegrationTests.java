/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.h2;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.h2.H2ConsoleAutoConfigurationIntegrationTests.TestConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link H2ConsoleAutoConfiguration}
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@WebAppConfiguration
@ContextConfiguration(classes = TestConfiguration.class)
@TestPropertySource(properties = "spring.h2.console.enabled:true")
public class H2ConsoleAutoConfigurationIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Test
	public void noPrincipal() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(springSecurity()).build();
		mockMvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
	}

	@Test
	public void userPrincipal() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(springSecurity()).build();
		mockMvc.perform(get("/h2-console/").with(user("test").roles("USER")))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
	}

	@Test
	public void someOtherPrincipal() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(springSecurity()).build();
		mockMvc.perform(get("/h2-console/").with(user("test").roles("FOO")))
				.andExpect(status().isForbidden());
	}

	@Configuration
	@Import({ SecurityAutoConfiguration.class, ServerPropertiesAutoConfiguration.class,
			H2ConsoleAutoConfiguration.class })
	@Controller
	static class TestConfiguration {

		@RequestMapping("/h2-console/**")
		public void mockConsole() {

		}

	}

}
