/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Integration tests for Endpoints over Spring MVC.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
public class WebMvcEndpointExposureIntegrationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					ServletWebServerFactoryAutoConfiguration.class,
					DispatcherServletAutoConfiguration.class,
					JacksonAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class,
					WebMvcAutoConfiguration.class, EndpointAutoConfiguration.class,
					WebEndpointAutoConfiguration.class,
					ManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class,
					ManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class))
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfigurationClasses.ALL));

	@Test
	public void webEndpointsAreDisabledByDefault() {
		this.contextRunner.run((context) -> {
			MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
			assertThat(isExposed(mvc, HttpMethod.GET, "beans")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "conditions")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "configprops")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "env")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "health")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "info")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "mappings")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.POST, "shutdown")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "status")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "threaddump")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "trace")).isFalse();
		});
	}

	@Test
	public void webEndpointsCanBeExposed() {
		WebApplicationContextRunner contextRunner = this.contextRunner
				.withPropertyValues("management.endpoints.web.expose=*");
		contextRunner.run((context) -> {
			MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
			assertThat(isExposed(mvc, HttpMethod.GET, "beans")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "conditions")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "configprops")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "env")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "health")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "info")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "mappings")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.POST, "shutdown")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "status")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "threaddump")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "trace")).isTrue();
		});
	}

	@Test
	public void singleWebEndpointCanBeExposed() {
		WebApplicationContextRunner contextRunner = this.contextRunner
				.withPropertyValues("management.endpoints.web.expose=beans");
		contextRunner.run((context) -> {
			MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
			assertThat(isExposed(mvc, HttpMethod.GET, "beans")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "conditions")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "configprops")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "env")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "health")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "info")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "mappings")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.POST, "shutdown")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "status")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "threaddump")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "trace")).isFalse();
		});
	}

	@Test
	public void singleWebEndpointCanBeExcluded() {
		WebApplicationContextRunner contextRunner = this.contextRunner.withPropertyValues(
				"management.endpoints.web.expose=*",
				"management.endpoints.web.exclude=shutdown");
		contextRunner.run((context) -> {
			MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
			assertThat(isExposed(mvc, HttpMethod.GET, "beans")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "conditions")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "configprops")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "env")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "health")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "info")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "mappings")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.POST, "shutdown")).isFalse();
			assertThat(isExposed(mvc, HttpMethod.GET, "status")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "threaddump")).isTrue();
			assertThat(isExposed(mvc, HttpMethod.GET, "trace")).isTrue();
		});
	}

	private boolean isExposed(MockMvc mockMvc, HttpMethod method, String path)
			throws Exception {
		path = "/application/" + path;
		MvcResult mvcResult = mockMvc.perform(request(method, path)).andReturn();
		int status = mvcResult.getResponse().getStatus();
		if (status == HttpStatus.OK.value()) {
			return true;
		}
		if (status == HttpStatus.NOT_FOUND.value()) {
			return false;
		}
		throw new IllegalStateException(String
				.format("Unexpected %s HTTP status for " + "endpoint %s", status, path));
	}

}
