/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.MinimalActuatorHypermediaApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link HalBrowserMvcEndpoint}
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = "management.security.enabled=false")
@DirtiesContext
public class HalBrowserMvcEndpointDisabledIntegrationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private MvcEndpoints mvcEndpoints;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void linksOnActuator() throws Exception {
		this.mockMvc.perform(get("/actuator").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists())
				.andExpect(header().doesNotExist("cache-control"));
	}

	@Test
	public void browserRedirect() throws Exception {
		this.mockMvc.perform(get("/actuator/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isFound()).andExpect(header().string(
						HttpHeaders.LOCATION, "http://localhost/actuator/browser.html"));
	}

	@Test
	public void endpointsDoNotHaveLinks() throws Exception {
		for (MvcEndpoint endpoint : this.mvcEndpoints.getEndpoints()) {
			String path = endpoint.getPath();
			if ("/actuator".equals(path) || endpoint instanceof HeapdumpMvcEndpoint) {
				continue;
			}
			path = (path.length() > 0) ? path : "/";
			MockHttpServletRequestBuilder requestBuilder = get(path);
			if (endpoint instanceof AuditEventsMvcEndpoint) {
				requestBuilder.param("after", "2016-01-01T12:00:00+00:00");
			}
			this.mockMvc.perform(requestBuilder.accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$._links").doesNotExist());
		}
	}

	@MinimalActuatorHypermediaApplication
	@Configuration
	public static class SpringBootHypermediaApplication {

		public static void main(String[] args) {
			SpringApplication.run(SpringBootHypermediaApplication.class,
					"--endpoints.hypermedia.enabled=false");
		}

	}

}
