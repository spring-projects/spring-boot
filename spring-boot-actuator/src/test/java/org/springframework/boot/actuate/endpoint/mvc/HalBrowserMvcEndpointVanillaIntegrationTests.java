/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.MinimalActuatorHypermediaApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
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
@DirtiesContext
@TestPropertySource(properties = "endpoints.hypermedia.enabled=true")
public class HalBrowserMvcEndpointVanillaIntegrationTests {

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
	public void links() throws Exception {
		this.mockMvc.perform(get("/actuator").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists())
				.andExpect(header().doesNotExist("cache-control"));
	}

	@Test
	public void linksWithTrailingSlash() throws Exception {
		this.mockMvc.perform(get("/actuator/").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists())
				.andExpect(header().doesNotExist("cache-control"));
	}

	@Test
	public void browser() throws Exception {
		MvcResult response = this.mockMvc
				.perform(get("/actuator/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk()).andReturn();
		assertThat(response.getResponse().getForwardedUrl())
				.isEqualTo("/actuator/browser.html");
	}

	@Test
	public void trace() throws Exception {
		this.mockMvc.perform(get("/trace").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$._links").doesNotExist())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	public void envValue() throws Exception {
		this.mockMvc.perform(get("/env/user.home").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links").doesNotExist());
	}

	@Test
	public void endpointsAllListed() throws Exception {
		for (MvcEndpoint endpoint : this.mvcEndpoints.getEndpoints()) {
			String path = endpoint.getPath();
			if ("/actuator".equals(path)) {
				continue;
			}
			path = path.startsWith("/") ? path.substring(1) : path;
			this.mockMvc.perform(get("/actuator").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$._links.%s.href", path).exists());
		}
	}

	@Test
	public void endpointsEachHaveSelf() throws Exception {
		Set<String> collections = new HashSet<String>(
				Arrays.asList("/trace", "/beans", "/dump", "/heapdump"));
		for (MvcEndpoint endpoint : this.mvcEndpoints.getEndpoints()) {
			String path = endpoint.getPath();
			if (collections.contains(path)) {
				continue;
			}
			path = path.length() > 0 ? path : "/";
			this.mockMvc.perform(get(path).accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk()).andExpect(jsonPath("$._links.self.href")
							.value("http://localhost" + endpoint.getPath()));
		}
	}

	@MinimalActuatorHypermediaApplication
	@Configuration
	public static class SpringBootHypermediaApplication {

	}

}
