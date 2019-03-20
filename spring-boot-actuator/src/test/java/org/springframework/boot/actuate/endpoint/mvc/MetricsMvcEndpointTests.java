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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link MetricsMvcEndpoint}
 *
 * @author Andy Wilkinson
 * @author Sergei Egorov
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest
@TestPropertySource(properties = "management.security.enabled=false")
public class MetricsMvcEndpointTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.context.getBean(MetricsEndpoint.class).setEnabled(true);
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void home() throws Exception {
		this.mvc.perform(get("/metrics")).andExpect(status().isOk())
				.andExpect(content().string(containsString("\"foo\":1")));
	}

	@Test
	public void homeContentTypeDefaultsToActuatorV1Json() throws Exception {
		this.mvc.perform(get("/metrics")).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.spring-boot.actuator.v1+json;charset=UTF-8"));
	}

	@Test
	public void homeContentTypeCanBeApplicationJson() throws Exception {
		this.mvc.perform(get("/metrics").header(HttpHeaders.ACCEPT,
				MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						MediaType.APPLICATION_JSON_UTF8_VALUE));
	}

	@Test
	public void specificMetricContentTypeDefaultsToActuatorV1Json() throws Exception {
		this.mvc.perform(get("/metrics/foo")).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.spring-boot.actuator.v1+json;charset=UTF-8"));
	}

	@Test
	public void specificMetricContentTypeCanBeApplicationJson() throws Exception {
		this.mvc.perform(get("/metrics/foo").header(HttpHeaders.ACCEPT,
				MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						MediaType.APPLICATION_JSON_UTF8_VALUE));
	}

	@Test
	public void homeWhenDisabled() throws Exception {
		this.context.getBean(MetricsEndpoint.class).setEnabled(false);
		this.mvc.perform(get("/metrics")).andExpect(status().isNotFound());
	}

	@Test
	public void specificMetric() throws Exception {
		this.mvc.perform(get("/metrics/foo")).andExpect(status().isOk())
				.andExpect(content().string(equalTo("{\"foo\":1}")));
	}

	@Test
	public void specificMetricWithNameThatCouldBeMistakenForAPathExtension()
			throws Exception {
		this.mvc.perform(get("/metrics/bar.png")).andExpect(status().isOk())
				.andExpect(content().string(equalTo("{\"bar.png\":1}")));
	}

	@Test
	public void specificMetricWhenDisabled() throws Exception {
		this.context.getBean(MetricsEndpoint.class).setEnabled(false);
		this.mvc.perform(get("/metrics/foo")).andExpect(status().isNotFound());
	}

	@Test
	public void specificMetricThatDoesNotExist() throws Exception {
		this.mvc.perform(get("/metrics/bar")).andExpect(status().isNotFound());
	}

	@Test
	public void regexAll() throws Exception {
		String expected = "\"foo\":1,\"bar.png\":1,\"group1.a\":1,\"group1.b\":1"
				+ ",\"group2.a\":1,\"group2_a\":1";
		this.mvc.perform(get("/metrics/.*")).andExpect(status().isOk())
				.andExpect(content().string(containsString(expected)));
	}

	@Test
	public void regexGroupDot() throws Exception {
		String expected = "\"group1.a\":1,\"group1.b\":1,\"group2.a\":1";
		this.mvc.perform(get("/metrics/group[0-9]+\\..*")).andExpect(status().isOk())
				.andExpect(content().string(containsString(expected)));
	}

	@Test
	public void regexGroup1() throws Exception {
		String expected = "\"group1.a\":1,\"group1.b\":1";
		this.mvc.perform(get("/metrics/group1\\..*")).andExpect(status().isOk())
				.andExpect(content().string(containsString(expected)));
	}

	@Test
	public void specificMetricWithDot() throws Exception {
		this.mvc.perform(get("/metrics/group2.a")).andExpect(status().isOk())
				.andExpect(content().string(containsString("1")));
	}

	@Import({ JacksonAutoConfiguration.class, AuditAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	@Configuration
	public static class TestConfiguration {

		@Bean
		public MetricsEndpoint endpoint() {
			return new MetricsEndpoint(new PublicMetrics() {

				@Override
				public Collection<Metric<?>> metrics() {
					ArrayList<Metric<?>> metrics = new ArrayList<Metric<?>>();
					metrics.add(new Metric<Integer>("foo", 1));
					metrics.add(new Metric<Integer>("bar.png", 1));
					metrics.add(new Metric<Integer>("group1.a", 1));
					metrics.add(new Metric<Integer>("group1.b", 1));
					metrics.add(new Metric<Integer>("group2.a", 1));
					metrics.add(new Metric<Integer>("group2_a", 1));
					metrics.add(new Metric<Integer>("baz", null));
					return Collections.unmodifiableList(metrics);
				}

			});
		}

	}

}
