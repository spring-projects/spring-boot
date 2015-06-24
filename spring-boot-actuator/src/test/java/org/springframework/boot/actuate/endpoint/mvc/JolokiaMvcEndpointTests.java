/*
 * Copyright 2013-2015 the original author or authors.
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

import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.JolokiaAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.mvc.JolokiaMvcEndpointTests.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link JolokiaMvcEndpoint}
 *
 * @author Christian Dupuis
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { Config.class })
@WebAppConfiguration
public class JolokiaMvcEndpointTests {

	@Autowired
	private MvcEndpoints endpoints;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		EnvironmentTestUtils.addEnvironment(
				(ConfigurableApplicationContext) this.context, "foo:bar");
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void endpointRegistered() throws Exception {
		Set<? extends MvcEndpoint> values = this.endpoints.getEndpoints();
		assertThat(values, (Matcher) hasItem(instanceOf(JolokiaMvcEndpoint.class)));
	}

	@Test
	public void search() throws Exception {
		this.mvc.perform(get("/jolokia/search/java.lang:*")).andExpect(status().isOk())
				.andExpect(content().string(containsString("GarbageCollector")));
	}

	@Test
	public void read() throws Exception {
		this.mvc.perform(get("/jolokia/read/java.lang:type=Memory"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("NonHeapMemoryUsage")));
	}

	@Test
	public void list() throws Exception {
		this.mvc.perform(get("/jolokia/list/java.lang/type=Memory/attr"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("NonHeapMemoryUsage")));
	}

	@Configuration
	@EnableConfigurationProperties
	@EnableWebMvc
	@Import({ EndpointWebMvcAutoConfiguration.class, JolokiaAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	public static class Config {

	}

}
