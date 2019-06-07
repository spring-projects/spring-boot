/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryHealthEndpointWebExtension}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundryHealthEndpointWebExtensionTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withPropertyValues("VCAP_APPLICATION={}")
			.withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class, WebMvcAutoConfiguration.class,
					JacksonAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
					RestTemplateAutoConfiguration.class, ManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class, EndpointAutoConfiguration.class,
					WebEndpointAutoConfiguration.class, HealthIndicatorAutoConfiguration.class,
					HealthEndpointAutoConfiguration.class, CloudFoundryActuatorAutoConfiguration.class));

	@Test
	public void healthDetailsAlwaysPresent() {
		this.contextRunner.run((context) -> {
			CloudFoundryHealthEndpointWebExtension extension = context
					.getBean(CloudFoundryHealthEndpointWebExtension.class);
			assertThat(extension.getHealth().getBody().getDetails()).isNotEmpty();
		});
	}

}
