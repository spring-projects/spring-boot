/*
 * Copyright 2012-2017 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.EnvironmentEndpoint;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link EnvironmentMvcEndpoint}
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = "management.security.enabled=false")
@DirtiesContext
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
	public void homeContentTypeDefaultsToActuatorV1Json() throws Exception {
		this.mvc.perform(get("/env")).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.spring-boot.actuator.v1+json;charset=UTF-8"));
	}

	@Test
	public void homeContentTypeCanBeApplicationJson() throws Exception {
		this.mvc.perform(
				get("/env").header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk()).andExpect(header().string("Content-Type",
						MediaType.APPLICATION_JSON_UTF8_VALUE));
	}

	@Test
	public void subContentTypeDefaultsToActuatorV1Json() throws Exception {
		this.mvc.perform(get("/env/foo")).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.spring-boot.actuator.v1+json;charset=UTF-8"));
	}

	@Test
	public void subContentTypeCanBeApplicationJson() throws Exception {
		this.mvc.perform(get("/env/foo").header(HttpHeaders.ACCEPT,
				MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						MediaType.APPLICATION_JSON_UTF8_VALUE));
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
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("food", null);
		((ConfigurableEnvironment) this.context.getEnvironment()).getPropertySources()
				.addFirst(new MapPropertySource("null-value", map));
		this.mvc.perform(get("/env/foo.*")).andExpect(status().isOk())
				.andExpect(content().string(containsString("\"foo\":\"bar\"")))
				.andExpect(content().string(containsString("\"fool\":\"baz\"")));
	}

	@Test
	public void nestedPathWhenPlaceholderCannotBeResolvedShouldReturnUnresolvedProperty()
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("my.foo", "${my.bar}");
		((ConfigurableEnvironment) this.context.getEnvironment()).getPropertySources()
				.addFirst(new MapPropertySource("unresolved-placeholder", map));
		this.mvc.perform(get("/env/my.foo")).andExpect(status().isOk())
				.andExpect(content().string(containsString("\"my.foo\":\"${my.bar}\"")));
	}

	@Test
	public void nestedPathWithSensitivePlaceholderShouldSanitize() throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("my.foo", "${my.password}");
		map.put("my.password", "hello");
		((ConfigurableEnvironment) this.context.getEnvironment()).getPropertySources()
				.addFirst(new MapPropertySource("placeholder", map));
		this.mvc.perform(get("/env/my.foo")).andExpect(status().isOk())
				.andExpect(content().string(containsString("\"my.foo\":\"******\"")));
	}

	@Test
	public void nestedPathMatchedByRegexWhenPlaceholderCannotBeResolvedShouldReturnUnresolvedProperty()
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("my.foo", "${my.bar}");
		((ConfigurableEnvironment) this.context.getEnvironment()).getPropertySources()
				.addFirst(new MapPropertySource("unresolved-placeholder", map));
		this.mvc.perform(get("/env/my.*")).andExpect(status().isOk())
				.andExpect(content().string(containsString("\"my.foo\":\"${my.bar}\"")));
	}

	@Test
	public void nestedPathMatchedByRegexWithSensitivePlaceholderShouldSanitize()
			throws Exception {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("my.foo", "${my.password}");
		map.put("my.password", "hello");
		((ConfigurableEnvironment) this.context.getEnvironment()).getPropertySources()
				.addFirst(new MapPropertySource("placeholder", map));
		this.mvc.perform(get("/env/my.*")).andExpect(status().isOk())
				.andExpect(content().string(containsString("\"my.foo\":\"******\"")));
	}

	@Test
	public void propertyWithTypeOtherThanStringShouldNotFail() throws Exception {
		MutablePropertySources propertySources = ((ConfigurableEnvironment) this.context
				.getEnvironment()).getPropertySources();
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("foo", Collections.singletonMap("bar", "baz"));
		propertySources.addFirst(new MapPropertySource("test", source));
		this.mvc.perform(get("/env/foo.*")).andExpect(status().isOk())
				.andExpect(content().string("{\"foo\":{\"bar\":\"baz\"}}"));
	}

	@Configuration
	@Import({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class, AuditAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	public static class TestConfiguration {

		@Bean
		public EnvironmentEndpoint endpoint() {
			return new EnvironmentEndpoint();
		}

	}

}
