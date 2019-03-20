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

import java.security.Principal;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.junit.runner.classpath.ClassPathExclusions;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the health endpoint when Spring Security is not available.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("spring-security-*.jar")
public class NoSpringSecurityHealthMvcEndpointIntegrationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void closeContext() {
		this.context.close();
	}

	@Test
	public void healthWhenRightRoleNotPresentShouldNotExposeHealthDetails()
			throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class);
		this.context.refresh();
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		mockMvc.perform(get("/health").with(getRequestPostProcessor()))
				.andExpect(status().isOk())
				.andExpect(content().string("{\"status\":\"UP\"}"));
	}

	@Test
	public void healthDetailPresent() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.security.enabled:false");
		this.context.refresh();
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		mockMvc.perform(get("/health")).andExpect(status().isOk())
				.andExpect(content().string(containsString(
						"\"status\":\"UP\",\"test\":{\"status\":\"UP\",\"hello\":\"world\"}")));
	}

	private RequestPostProcessor getRequestPostProcessor() {
		return new RequestPostProcessor() {

			@Override
			public MockHttpServletRequest postProcessRequest(
					MockHttpServletRequest request) {
				Principal principal = mock(Principal.class);
				request.setUserPrincipal(principal);
				return request;
			}

		};
	}

	@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, EndpointAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class })
	@Configuration
	static class TestConfiguration {

		@Bean
		public HealthIndicator testHealthIndicator() {
			return new HealthIndicator() {

				@Override
				public Health health() {
					return Health.up().withDetail("hello", "world").build();
				}

			};
		}

	}

}
