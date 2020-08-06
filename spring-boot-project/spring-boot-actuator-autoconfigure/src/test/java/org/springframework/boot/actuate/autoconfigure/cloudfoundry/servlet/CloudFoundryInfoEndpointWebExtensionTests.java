/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CloudFoundryInfoEndpointWebExtension}.
 *
 * @author Madhura Bhave
 */
class CloudFoundryInfoEndpointWebExtensionTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withPropertyValues("VCAP_APPLICATION={}")
			.withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class, WebMvcAutoConfiguration.class,
					JacksonAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
					HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
					RestTemplateAutoConfiguration.class, ManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class, EndpointAutoConfiguration.class,
					WebEndpointAutoConfiguration.class, ProjectInfoAutoConfiguration.class,
					InfoContributorAutoConfiguration.class, InfoEndpointAutoConfiguration.class,
					HealthEndpointAutoConfiguration.class, CloudFoundryActuatorAutoConfiguration.class));

	@Test
	@SuppressWarnings("unchecked")
	void gitFullDetailsAlwaysPresent() {
		this.contextRunner.run((context) -> {
			CloudFoundryInfoEndpointWebExtension extension = context
					.getBean(CloudFoundryInfoEndpointWebExtension.class);
			Map<String, Object> git = (Map<String, Object>) extension.info().get("git");
			Map<String, Object> commit = (Map<String, Object>) git.get("commit");
			assertThat(commit).hasSize(4);
		});
	}

}
