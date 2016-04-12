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

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.JolokiaAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
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
 * Integration tests for the actuator endpoints' CORS support
 *
 * @author Andy Wilkinson
 */
public class MvcEndpointCorsIntegrationTests {

	private AnnotationConfigWebApplicationContext context;

	@Before
	public void createContext() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				JolokiaAutoConfiguration.class, WebMvcAutoConfiguration.class);
	}

	@Test
	public void corsIsDisabledByDefault() throws Exception {
		createMockMvc()
				.perform(options("/beans").header("Origin", "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(
						header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void settingAllowedOriginsEnablesCors() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com");
		createMockMvc()
				.perform(options("/beans").header("Origin", "bar.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden());
		performAcceptedCorsRequest();
	}

	@Test
	public void maxAgeDefaultsTo30Minutes() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com");
		performAcceptedCorsRequest()
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "1800"));
	}

	@Test
	public void maxAgeCanBeConfigured() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com",
				"endpoints.cors.max-age: 2400");
		performAcceptedCorsRequest()
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "2400"));
	}

	@Test
	public void requestsWithDisallowedHeadersAreRejected() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com");
		createMockMvc()
				.perform(options("/beans").header("Origin", "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha"))
				.andExpect(status().isForbidden());
	}

	@Test
	public void allowedHeadersCanBeConfigured() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com",
				"endpoints.cors.allowed-headers:Alpha,Bravo");
		createMockMvc()
				.perform(options("/beans").header("Origin", "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Alpha"))
				.andExpect(status().isOk()).andExpect(header()
						.string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Alpha"));
	}

	@Test
	public void requestsWithDisallowedMethodsAreRejected() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com");
		createMockMvc()
				.perform(options("/health").header(HttpHeaders.ORIGIN, "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
				.andExpect(status().isForbidden());
	}

	@Test
	public void allowedMethodsCanBeConfigured() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com",
				"endpoints.cors.allowed-methods:GET,HEAD");
		createMockMvc()
				.perform(options("/health").header(HttpHeaders.ORIGIN, "foo.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "HEAD"))
				.andExpect(status().isOk()).andExpect(header()
						.string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,HEAD"));
	}

	@Test
	public void credentialsCanBeAllowed() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com",
				"endpoints.cors.allow-credentials:true");
		performAcceptedCorsRequest().andExpect(
				header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	public void credentialsCanBeDisabled() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com",
				"endpoints.cors.allow-credentials:false");
		performAcceptedCorsRequest().andExpect(
				header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	public void jolokiaEndpointUsesGlobalCorsConfiguration() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.cors.allowed-origins:foo.example.com");
		createMockMvc()
				.perform(options("/jolokia").header("Origin", "bar.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden());
		performAcceptedCorsRequest("/jolokia");
	}

	private MockMvc createMockMvc() {
		this.context.refresh();
		return MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	private ResultActions performAcceptedCorsRequest() throws Exception {
		return performAcceptedCorsRequest("/beans");
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
