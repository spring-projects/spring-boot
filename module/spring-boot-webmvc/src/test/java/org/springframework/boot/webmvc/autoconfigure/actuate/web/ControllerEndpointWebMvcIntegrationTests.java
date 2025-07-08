/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.audit.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Actuator's MVC
 * {@link org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpoint
 * controller endpoints}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ControllerEndpointWebMvcIntegrationTests {

	private AnnotationConfigServletWebApplicationContext context;

	@AfterEach
	void close() {
		this.context.close();
	}

	@Test
	void endpointsCanBeAccessed() {
		this.context = new AnnotationConfigServletWebApplicationContext();
		this.context.register(DefaultConfiguration.class, ExampleController.class);
		TestPropertyValues
			.of("management.endpoints.web.base-path:/management", "management.endpoints.web.exposure.include=*")
			.applyTo(this.context);
		MockMvcTester mvc = createMockMvcTester();
		assertThat(mvc.get().uri("/management/example")).hasStatusOk();
	}

	private MockMvcTester createMockMvcTester() {
		this.context.setServletContext(new MockServletContext());
		this.context.refresh();
		return MockMvcTester.from(this.context);
	}

	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
			ServletManagementContextAutoConfiguration.class, AuditAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementContextAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			BeansEndpointAutoConfiguration.class })
	static class DefaultConfiguration {

	}

	@org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint(id = "example")
	@SuppressWarnings("removal")
	static class ExampleController {

		@GetMapping("/")
		String example() {
			return "Example";
		}

	}

}
