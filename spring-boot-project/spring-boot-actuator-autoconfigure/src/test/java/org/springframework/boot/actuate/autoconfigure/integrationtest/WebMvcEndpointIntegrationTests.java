/*
 * Copyright 2012-2021 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
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
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Actuator's MVC endpoints.
 *
 * @author Andy Wilkinson
 */
class WebMvcEndpointIntegrationTests {

	private AnnotationConfigServletWebApplicationContext context;

	@AfterEach
	void close() {
		TestSecurityContextHolder.clearContext();
		this.context.close();
	}

	@Test
	void endpointsAreSecureByDefault() throws Exception {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/actuator/beans").accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());
	}

	@Test
	void endpointsAreSecureByDefaultWithCustomBasePath() throws Exception {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.base-path:/management").applyTo(this.context);
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/management/beans").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void endpointsAreSecureWithActuatorRoleWithCustomBasePath() throws Exception {
		TestSecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("user", "N/A", "ROLE_ACTUATOR"));
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		TestPropertyValues
				.of("management.endpoints.web.base-path:/management", "management.endpoints.web.exposure.include=*")
				.applyTo(this.context);
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/management/beans")).andExpect(status().isOk());
	}

	@Test
	void linksAreProvidedToAllEndpointTypes() throws Exception {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(DefaultConfiguration.class, EndpointsConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.exposure.include=*").applyTo(this.context);
		MockMvc mockMvc = doCreateMockMvc();
		mockMvc.perform(get("/actuator").accept("*/*")).andExpect(status().isOk()).andExpect(jsonPath("_links",
				both(hasKey("beans")).and(hasKey("servlet")).and(hasKey("restcontroller")).and(hasKey("controller"))));
	}

	@Test
	void linksPageIsNotAvailableWhenDisabled() throws Exception {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(DefaultConfiguration.class, EndpointsConfiguration.class);
		TestPropertyValues.of("management.endpoints.web.discovery.enabled=false").applyTo(this.context);
		MockMvc mockMvc = doCreateMockMvc();
		mockMvc.perform(get("/actuator").accept("*/*")).andExpect(status().isNotFound());
	}

	private MockMvc createSecureMockMvc() {
		return doCreateMockMvc(springSecurity());
	}

	private MockMvc doCreateMockMvc(MockMvcConfigurer... configurers) {
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.context);
		for (MockMvcConfigurer configurer : configurers) {
			builder.apply(configurer);
		}
		return builder.build();
	}

	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
			ServletManagementContextAutoConfiguration.class, AuditAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementContextAutoConfiguration.class, AuditAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, BeansEndpointAutoConfiguration.class })
	static class DefaultConfiguration {

	}

	@Import(SecureConfiguration.class)
	@ImportAutoConfiguration({ HypermediaAutoConfiguration.class })
	static class SpringHateoasConfiguration {

	}

	@Import(SecureConfiguration.class)
	@ImportAutoConfiguration({ HypermediaAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class })
	static class SpringDataRestConfiguration {

	}

	@Import(DefaultConfiguration.class)
	@ImportAutoConfiguration({ SecurityAutoConfiguration.class })
	static class SecureConfiguration {

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
