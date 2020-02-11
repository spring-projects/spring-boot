/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import java.util.function.Supplier;

import javax.servlet.http.HttpServlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.audit.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.EndpointServlet;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Actuator's MVC endpoints.
 *
 * @author Andy Wilkinson
 * @author Brian Clozel
 */
public class WebMvcEndpointIntegrationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, GsonAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, EndpointAutoConfiguration.class,
					WebEndpointAutoConfiguration.class, ServletManagementContextAutoConfiguration.class,
					AuditAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
					WebMvcAutoConfiguration.class, ManagementContextAutoConfiguration.class,
					AuditAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
					BeansEndpointAutoConfiguration.class));

	@Test
	void linksAreProvidedToAllEndpointTypes() throws Exception {
		this.contextRunner.withUserConfiguration(EndpointsConfiguration.class)
				.withPropertyValues("management.endpoints.web.exposure.include=*").run((context) -> {
					MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
					mockMvc.perform(get("/actuator").accept("*/*")).andExpect(status().isOk())
							.andExpect(jsonPath("_links", both(hasKey("beans")).and(hasKey("servlet"))
									.and(hasKey("restcontroller")).and(hasKey("controller"))));
				});
	}

	@Test
	void dedicatedJsonMapperIsUsed() throws Exception {
		this.contextRunner.withPropertyValues("spring.mvc.converters.preferred-json-mapper:gson",
				"management.endpoints.web.exposure.include=*").run((context) -> {
					MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
					mockMvc.perform(get("/actuator/beans").accept("*/*")).andExpect(status().isOk())
							.andExpect(MockMvcResultMatchers.header().string("Content-Type",
									startsWith("application/vnd.spring-boot.actuator")));
				});
	}

	@ServletEndpoint(id = "servlet")
	static class TestServletEndpoint implements Supplier<EndpointServlet> {

		@Override
		public EndpointServlet get() {
			return new EndpointServlet(new HttpServlet() {
			});
		}

	}

	@ControllerEndpoint(id = "controller")
	static class TestControllerEndpoint {

	}

	@RestControllerEndpoint(id = "restcontroller")
	static class TestRestControllerEndpoint {

	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointsConfiguration {

		@Bean
		TestServletEndpoint testServletEndpoint() {
			return new TestServletEndpoint();
		}

		@Bean
		TestControllerEndpoint testControllerEndpoint() {
			return new TestControllerEndpoint();
		}

		@Bean
		TestRestControllerEndpoint testRestControllerEndpoint() {
			return new TestRestControllerEndpoint();
		}

	}

}
