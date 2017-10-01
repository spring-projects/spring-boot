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

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.audit.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Actuator's MVC endpoints.
 *
 * @author Andy Wilkinson
 */
public class WebMvcEndpointIntegrationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		TestSecurityContextHolder.clearContext();
		this.context.close();
	}

	@Test
	public void endpointsAreSecureByDefault() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/application/beans").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void endpointsAreSecureByDefaultWithCustomContextPath() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		TestPropertyValues.of("management.context-path:/management")
				.applyTo(this.context);
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/management/beans").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void endpointsAreSecureWithActuatorRoleWithCustomContextPath()
			throws Exception {
		TestSecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("user", "N/A", "ROLE_ACTUATOR"));
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		TestPropertyValues.of("management.context-path:/management",
				"endpoints.default.web.enabled=true").applyTo(this.context);
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/management/beans")).andExpect(status().isOk());
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

	@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, EndpointAutoConfiguration.class,
			ServletManagementContextAutoConfiguration.class, AuditAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementContextAutoConfiguration.class, AuditAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class,
			BeansEndpointAutoConfiguration.class })
	static class DefaultConfiguration {

	}

	@Import(SecureConfiguration.class)
	@ImportAutoConfiguration({ HypermediaAutoConfiguration.class })
	static class SpringHateoasConfiguration {

	}

	@Import(SecureConfiguration.class)
	@ImportAutoConfiguration({ HypermediaAutoConfiguration.class,
			RepositoryRestMvcAutoConfiguration.class })
	static class SpringDataRestConfiguration {

	}

	@Import(DefaultConfiguration.class)
	@ImportAutoConfiguration({ SecurityAutoConfiguration.class })
	static class SecureConfiguration {

	}

}
