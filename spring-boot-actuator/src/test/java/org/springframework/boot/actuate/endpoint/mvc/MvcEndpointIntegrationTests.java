/*
 * Copyright 2012-2015 the original author or authors.
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

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.test.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the Actuator's MVC endpoints.
 *
 * @author Andy Wilkinson
 */
public class MvcEndpointIntegrationTests {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		TestSecurityContextHolder.clearContext();
		this.context.close();
	}

	@Test
	public void defaultJsonResponseIsNotIndented() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(DefaultConfiguration.class);
		MockMvc mockMvc = createMockMvc();
		mockMvc.perform(get("/beans")).andExpect(content().string(startsWith("{\"")));
	}

	@Test
	public void jsonResponsesCanBeIndented() throws Exception {
		assertIndentedJsonResponse(DefaultConfiguration.class);
	}

	@Test
	public void jsonResponsesCanBeIndentedWhenSpringHateoasIsAutoConfigured()
			throws Exception {
		assertIndentedJsonResponse(SpringHateoasConfiguration.class);
	}

	@Test
	public void jsonResponsesCanBeIndentedWhenSpringDataRestIsAutoConfigured()
			throws Exception {
		assertIndentedJsonResponse(SpringDataRestConfiguration.class);
	}

	@Test
	public void fileExtensionNotFound() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(DefaultConfiguration.class);
		MockMvc mockMvc = createMockMvc();
		mockMvc.perform(get("/beans.cmd")).andExpect(status().isNotFound());
	}

	@Test
	public void jsonExtensionProvided() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(DefaultConfiguration.class);
		MockMvc mockMvc = createMockMvc();
		mockMvc.perform(get("/beans.json")).andExpect(status().isOk());
	}

	@Test
	public void nonSensitiveEndpointsAreNotSecureByDefault() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/info")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator")).andExpect(status().isOk());
	}

	@Test
	public void nonSensitiveEndpointsAreNotSecureByDefaultWithCustomContextPath()
			throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.context-path:/management");
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/management/info")).andExpect(status().isOk());
		mockMvc.perform(get("/management/")).andExpect(status().isOk());
	}

	@Test
	public void sensitiveEndpointsAreSecureByDefault() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/beans")).andExpect(status().isUnauthorized());
	}

	@Test
	public void sensitiveEndpointsAreSecureByDefaultWithCustomContextPath()
			throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.context-path:/management");
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/management/beans")).andExpect(status().isUnauthorized());
	}

	@Test
	public void sensitiveEndpointsAreSecureWithNonAdminRoleWithCustomContextPath()
			throws Exception {
		TestSecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("user", "N/A", "ROLE_USER"));
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.context-path:/management");
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/management/beans")).andExpect(status().isForbidden());
	}

	@Test
	public void sensitiveEndpointsAreSecureWithAdminRoleWithCustomContextPath()
			throws Exception {
		TestSecurityContextHolder.getContext().setAuthentication(
				new TestingAuthenticationToken("user", "N/A", "ROLE_ADMIN"));
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.context-path:/management");
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/management/beans")).andExpect(status().isOk());
	}

	@Test
	public void endpointSecurityCanBeDisabledWithCustomContextPath() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.context-path:/management",
				"management.security.enabled:false");
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/management/beans")).andExpect(status().isOk());
	}

	@Test
	public void endpointSecurityCanBeDisabled() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(SecureConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.security.enabled:false");
		MockMvc mockMvc = createSecureMockMvc();
		mockMvc.perform(get("/beans")).andExpect(status().isOk());
	}

	private void assertIndentedJsonResponse(Class<?> configuration) throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(configuration);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.serialization.indent-output:true");
		MockMvc mockMvc = createMockMvc();
		mockMvc.perform(get("/beans"))
				.andExpect(content().string(startsWith("{" + LINE_SEPARATOR)));
	}

	private MockMvc createMockMvc() {
		return doCreateMockMvc();
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
			EndpointWebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class })
	static class DefaultConfiguration {

	}

	@ImportAutoConfiguration({ HypermediaAutoConfiguration.class,
			JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class })
	static class SpringHateoasConfiguration {

	}

	@ImportAutoConfiguration({ HypermediaAutoConfiguration.class,
			RepositoryRestMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, EndpointAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class })
	static class SpringDataRestConfiguration {

	}

	@Import(DefaultConfiguration.class)
	@ImportAutoConfiguration({ SecurityAutoConfiguration.class,
			ManagementWebSecurityAutoConfiguration.class })
	static class SecureConfiguration {

	}

}
