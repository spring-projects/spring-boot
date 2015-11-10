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
import org.springframework.boot.actuate.autoconfigure.MinimalActuatorHypermediaApplication;
import org.springframework.boot.actuate.endpoint.mvc.HalBrowserMvcEndpointManagementContextPathIntegrationTests.SpringBootHypermediaApplication;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link HalBrowserMvcEndpoint} when a custom management
 * context path has been configured.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(SpringBootHypermediaApplication.class)
@WebAppConfiguration
@TestPropertySource(properties = "management.contextPath:/admin")
@DirtiesContext
public class HalBrowserMvcEndpointManagementContextPathIntegrationTests {

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
	public void actuatorHomeJson() throws Exception {
		this.mockMvc.perform(get("/admin").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$._links").exists());
	}

	@Test
	public void actuatorHomeHtml() throws Exception {
		this.mockMvc.perform(get("/admin/").accept(MediaType.TEXT_HTML))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/admin/browser.html"));
	}

	@Test
	public void trace() throws Exception {
		this.mockMvc.perform(get("/admin/trace").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.self.href")
						.value("http://localhost/admin/trace"))
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	public void endpointsAllListed() throws Exception {
		for (MvcEndpoint endpoint : this.mvcEndpoints.getEndpoints()) {
			String path = endpoint.getPath();
			if ("/actuator".equals(path)) {
				continue;
			}
			path = path.startsWith("/") ? path.substring(1) : path;
			path = path.length() > 0 ? path : "self";
			this.mockMvc.perform(get("/admin").accept(MediaType.APPLICATION_JSON))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$._links.%s.href", path)
							.value("http://localhost/admin" + endpoint.getPath()));
		}
	}

	@MinimalActuatorHypermediaApplication
	@RestController
	public static class SpringBootHypermediaApplication {

		@RequestMapping("")
		public ResourceSupport home() {
			ResourceSupport resource = new ResourceSupport();
			resource.add(linkTo(SpringBootHypermediaApplication.class).slash("/")
					.withSelfRel());
			return resource;
		}

	}

}
