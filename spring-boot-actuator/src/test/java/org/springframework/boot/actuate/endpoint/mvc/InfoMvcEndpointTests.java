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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 * Tests for {@link InfoEndpoint} with {@link MockMvc}.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = { "info.app.name=MyService" })
public class InfoMvcEndpointTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.context.getBean(InfoEndpoint.class).setEnabled(true);
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void home() throws Exception {
		this.mvc.perform(get("/info")).andExpect(status().isOk())
				.andExpect(content().string(containsString(
						"\"beanName1\":{\"key11\":\"value11\",\"key12\":\"value12\"}")))
				.andExpect(content().string(containsString(
						"\"beanName2\":{\"key21\":\"value21\",\"key22\":\"value22\"}")));
	}

	@Test
	public void contentTypeDefaultsToActuatorV1Json() throws Exception {
		this.mvc.perform(get("/info")).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.spring-boot.actuator.v1+json;charset=UTF-8"));
	}

	@Test
	public void contentTypeCanBeApplicationJson() throws Exception {
		this.mvc.perform(
				get("/info").header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
				.andExpect(status().isOk()).andExpect(header().string("Content-Type",
						MediaType.APPLICATION_JSON_UTF8_VALUE));
	}

	@Import({ JacksonAutoConfiguration.class, AuditAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	@Configuration
	public static class TestConfiguration {

		@Bean
		public InfoEndpoint endpoint() {
			return new InfoEndpoint(Arrays.asList(beanName1(), beanName2()));
		}

		@Bean
		public InfoContributor beanName1() {
			return new InfoContributor() {

				@Override
				public void contribute(Info.Builder builder) {
					Map<String, Object> content = new LinkedHashMap<String, Object>();
					content.put("key11", "value11");
					content.put("key12", "value12");
					builder.withDetail("beanName1", content);
				}
			};
		}

		@Bean
		public InfoContributor beanName2() {
			return new InfoContributor() {
				@Override
				public void contribute(Info.Builder builder) {
					Map<String, Object> content = new LinkedHashMap<String, Object>();
					content.put("key21", "value21");
					content.put("key22", "value22");
					builder.withDetail("beanName2", content);
				}
			};
		}

	}

}
