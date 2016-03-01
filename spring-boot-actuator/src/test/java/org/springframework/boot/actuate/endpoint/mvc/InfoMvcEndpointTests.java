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

import java.util.Map;

import org.elasticsearch.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.InfoEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.InfoMvcEndpointTests.TestConfiguration;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoProvider;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
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
 * Tests for {@link InfoMvcEndpointTests}
 *
 * @author Meang Akira Tanaka
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {TestConfiguration.class})
@WebAppConfiguration
@TestPropertySource(properties = {"info.app.name=MyService"})
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
				.andExpect(content().string(
						containsString("\"beanName2\":{\"key22\":\"value22\",\"key21\":\"value21\"}," +
								"\"beanName1\":{\"key12\":\"value12\",\"key11\":\"value11\"}")));
	}

	@Import({JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class,
			WebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class})
	@Configuration
	public static class TestConfiguration {

		private Map<String, InfoProvider> infoProviders = Maps.newHashMap();

		public TestConfiguration() {
			InfoProvider infoProvider1 = new InfoProvider() {

				@Override
				public Info provide() {
					Info result = new Info();
					result.put("key11", "value11");
					result.put("key12", "value12");
					return result;
				}

				@Override
				public String name() {
					return "beanName1";
				}
			};
			this.infoProviders.put("beanName1", infoProvider1);

			InfoProvider infoProvider2 = new InfoProvider() {

				@Override
				public Info provide() {
					Info result = new Info();
					result.put("key21", "value21");
					result.put("key22", "value22");
					return result;
				}

				@Override
				public String name() {
					return "beanName2";
				}
			};
			this.infoProviders.put("beanName2", infoProvider2);
		}

		@Bean
		public InfoEndpoint endpoint() {
			return new InfoEndpoint(this.infoProviders);
		}
	}

}
