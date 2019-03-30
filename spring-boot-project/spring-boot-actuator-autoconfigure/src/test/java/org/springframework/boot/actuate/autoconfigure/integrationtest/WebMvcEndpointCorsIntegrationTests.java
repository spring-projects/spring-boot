/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the MVC actuator endpoints' CORS support
 *
 * @author Andy Wilkinson
 * @see WebMvcEndpointManagementContextConfiguration
 */
public class WebMvcEndpointCorsIntegrationTests {

	private AnnotationConfigWebApplicationContext context;

	@Before
	public void createContext() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
				EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
				ManagementContextAutoConfiguration.class,
				ServletManagementContextAutoConfiguration.class,
				BeansEndpointAutoConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.exposure.include:*")
				.applyTo(this.context);
	}

	@Test
	public void corsIsDisabledByDefault() throws Exception {
		createMockMvc()
				.perform(options("/actuator/beans").header("Origin", "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(
						header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void settingAllowedOriginsEnablesCors() throws Exception {
		TestPropertyValues
				.of("management.endpoints.web.cors.allowed-origins:foo.example.com")
				.applyTo(this.context);
		createMockMvc()
				.perform(options("/actuator/beans").header("Origin", "bar.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden());
		performAcceptedCorsRequest();
	}

	@Test
	public void maxAgeDefaultsTo30Minutes() throws Exception {
		TestPropertyValues
				.of("management.endpoints.web.cors.allowed-origins:foo.example.com")
				.applyTo(this.context);
		performAcceptedCorsRequest()
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800"));
	}

	@Test
	public void maxAgeCanBeConfigured() throws Exception {
		TestPropertyValues
				.of("management.endpoints.web.cors.allowed-origins:foo.example.com",
						"management.endpoints.web.cors.max-age: 2400")
				.applyTo(this.context);
		performAcceptedCorsRequest()
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "2400"));
	}

	@Test
	public void requestsWithDisallowedHeadersAreRejected() throws Exception {
		TestPropertyValues
				.of("management.endpoints.web.cors.allowed-origins:foo.example.com")
				.applyTo(this.context);
		createMockMvc()
				.perform(options("/actuator/beans").header("Origin", "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha"))
				.andExpect(status().isForbidden());
	}

	@Test
	public void allowedHeadersCanBeConfigured() throws Exception {
		TestPropertyValues
				.of("management.endpoints.web.cors.allowed-origins:foo.example.com",
						"management.endpoints.web.cors.allowed-headers:Alpha,Bravo")
				.applyTo(this.context);
		createMockMvc()
				.perform(options("/actuator/beans").header("Origin", "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha"))
				.andExpect(status().isOk()).andExpect(header()
						.string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Alpha"));
	}

	@Test
	public void requestsWithDisallowedMethodsAreRejected() throws Exception {
		TestPropertyValues
				.of("management.endpoints.web.cors.allowed-origins:foo.example.com")
				.applyTo(this.context);
		createMockMvc()
				.perform(options("/actuator/health")
						.header(HttpHeaders.ORIGIN, "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
				.andExpect(status().isForbidden());
	}

	@Test
	public void allowedMethodsCanBeConfigured() throws Exception {
		TestPropertyValues
				.of("management.endpoints.web.cors.allowed-origins:foo.example.com",
						"management.endpoints.web.cors.allowed-methods:GET,HEAD")
				.applyTo(this.context);
		createMockMvc()
				.perform(options("/actuator/beans")
						.header(HttpHeaders.ORIGIN, "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "HEAD"))
				.andExpect(status().isOk()).andExpect(header()
						.string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD"));
	}

	@Test
	public void credentialsCanBeAllowed() throws Exception {
		TestPropertyValues
				.of("management.endpoints.web.cors.allowed-origins:foo.example.com",
						"management.endpoints.web.cors.allow-credentials:true")
				.applyTo(this.context);
		performAcceptedCorsRequest().andExpect(
				header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	public void credentialsCanBeDisabled() throws Exception {
		TestPropertyValues
				.of("management.endpoints.web.cors.allowed-origins:foo.example.com",
						"management.endpoints.web.cors.allow-credentials:false")
				.applyTo(this.context);
		performAcceptedCorsRequest().andExpect(
				header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	private MockMvc createMockMvc() {
		this.context.refresh();
		return MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	private ResultActions performAcceptedCorsRequest() throws Exception {
		return performAcceptedCorsRequest("/actuator/beans");
	}

	private ResultActions performAcceptedCorsRequest(String url) throws Exception {
		return createMockMvc()
				.perform(options(url).header(HttpHeaders.ORIGIN, "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
						"foo.example.com"))
				.andExpect(status().isOk());
	}

}
