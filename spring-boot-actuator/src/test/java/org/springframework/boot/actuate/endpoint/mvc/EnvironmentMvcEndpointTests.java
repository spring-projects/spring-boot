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

package org.springframework.boot.actuate.endpoint.mvc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EnvironmentMvcEndpointTests.TestConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link EnvironmentMvcEndpoint}
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@WebAppConfiguration
public class EnvironmentMvcEndpointTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.context.getBean(EnvironmentEndpoint.class).setEnabled(true);
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		EnvironmentTestUtils.addEnvironment((ConfigurableApplicationContext) this.context,
				"foo:bar", "fool:baz");
	}

	@Test
	public void home() throws Exception {
		this.mvc.perform(get("/env")).andExpect(status().isOk())
				.andExpect(content().string(containsString("systemProperties")));
	}

	@Test
	public void sub() throws Exception {
		this.mvc.perform(get("/env/foo")).andExpect(status().isOk())
				.andExpect(content().string("{\"foo\":\"bar\"}"));
	}

	@Test
	public void subWhenDisabled() throws Exception {
		this.context.getBean(EnvironmentEndpoint.class).setEnabled(false);
		this.mvc.perform(get("/env/foo")).andExpect(status().isNotFound());
	}

	@Test
	public void regex() throws Exception {
		this.mvc.perform(get("/env/foo.*")).andExpect(status().isOk())
				.andExpect(content().string(containsString("\"foo\":\"bar\"")))
				.andExpect(content().string(containsString("\"fool\":\"baz\"")));
	}

	@Import({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	@Configuration
	public static class TestConfiguration {

		@Bean
		public EnvironmentEndpoint endpoint() {
			return new EnvironmentEndpoint();
		}

	}

}
