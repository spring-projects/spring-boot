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

import org.junit.Test;
import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.test.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Integration tests for the Actuator's MVC endpoints.
 *
 * @author Andy Wilkinson
 */
public class MvcEndpointIntegrationTests {

	private AnnotationConfigWebApplicationContext context;

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

	private void assertIndentedJsonResponse(Class<?> configuration) throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(configuration);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jackson.serialization.indent-output:true");
		MockMvc mockMvc = createMockMvc();
		mockMvc.perform(get("/beans")).andExpect(content().string(startsWith("{\n")));
	}

	private MockMvc createMockMvc() {
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		return MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
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
			HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class })
	static class SpringDataRestConfiguration {

	}

}
