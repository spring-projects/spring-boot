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
package org.springframework.boot.actuate.autoconfigure.web.server;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementContextAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class ManagementContextAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class));

	@Rule
	public OutputCapture output = new OutputCapture();

	@Test
	public void managementServerPortShouldBeIgnoredForNonEmbeddedServer() {
		this.contextRunner.withPropertyValues("management.server.port=8081").run((context) -> {
			assertThat(context.getStartupFailure()).isNull();
			assertThat(this.output.toString()).contains("Could not start embedded management container on "
					+ "different port (management endpoints are still available through JMX)");
		});
	}

	@Test
	public void childManagementContextShouldStartForEmbeddedServer() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ManagementContextAutoConfiguration.class,
								ServletWebServerFactoryAutoConfiguration.class,
								ServletManagementContextAutoConfiguration.class, WebEndpointAutoConfiguration.class,
								EndpointAutoConfiguration.class));
		contextRunner.withPropertyValues("management.server.port=8081")
				.run((context) -> assertThat(this.output.toString())
						.doesNotContain("Could not start embedded management container on "
								+ "different port (management endpoints are still available through JMX)"));
	}

}
