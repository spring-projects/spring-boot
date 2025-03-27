/*
 * Copyright 2012-2025 the original author or authors.
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
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.http.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;

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
	@WithResource(name = "git.properties", content = """
			#Generated by Git-Commit-Id-Plugin
			#Thu May 23 09:26:42 BST 2013
			git.commit.id.abbrev=e02a4f3
			git.commit.user.email=dsyer@vmware.com
			git.commit.message.full=Update Spring
			git.commit.id=e02a4f3b6f452cdbf6dd311f1362679eb4c31ced
			git.commit.message.short=Update Spring
			git.commit.user.name=Dave Syer
			git.build.user.name=Dave Syer
			git.build.user.email=dsyer@vmware.com
			git.branch=develop
			git.commit.time=2013-04-24T08\\:42\\:13+0100
			git.build.time=2013-05-23T09\\:26\\:42+0100
			""")
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
