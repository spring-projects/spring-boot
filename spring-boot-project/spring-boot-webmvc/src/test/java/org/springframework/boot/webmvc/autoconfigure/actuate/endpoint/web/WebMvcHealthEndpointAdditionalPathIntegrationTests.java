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

package org.springframework.boot.webmvc.autoconfigure.actuate.endpoint.web;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.integrationtest.AbstractHealthEndpointAdditionalPathIntegrationTests;
import org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.servlet.autoconfigure.actuate.web.ServletManagementContextAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.autoconfigure.actuate.web.server.TomcatServletManagementContextAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.web.context.ConfigurableWebApplicationContext;

/**
 * Integration tests for MVC health groups on an additional path.
 *
 * @author Madhura Bhave
 */
class WebMvcHealthEndpointAdditionalPathIntegrationTests extends
		AbstractHealthEndpointAdditionalPathIntegrationTests<WebApplicationContextRunner, ConfigurableWebApplicationContext, AssertableWebApplicationContext> {

	WebMvcHealthEndpointAdditionalPathIntegrationTests() {
		super(new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
					HealthContributorAutoConfiguration.class, HealthContributorRegistryAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, ManagementContextAutoConfiguration.class,
					TomcatServletWebServerAutoConfiguration.class, TomcatServletWebServerAutoConfiguration.class,
					TomcatServletManagementContextAutoConfiguration.class, WebMvcAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					EndpointAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
					HealthEndpointAutoConfiguration.class, WebMvcHealthEndpointExtensionAutoConfiguration.class,
					DiskSpaceHealthContributorAutoConfiguration.class))
			.withInitializer(new ServerPortInfoApplicationContextInitializer())
			.withPropertyValues("server.port=0"));
	}

}
