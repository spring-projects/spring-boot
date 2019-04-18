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

package org.springframework.boot.actuate.autoconfigure.jolokia;

import java.util.Collection;
import java.util.Collections;

import org.jolokia.http.AgentServlet;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.ServletEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.actuate.endpoint.web.ExposableServletEndpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JolokiaEndpointAutoConfiguration}.
 *
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class JolokiaEndpointAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					DispatcherServletAutoConfiguration.class,
					ManagementContextAutoConfiguration.class,
					ServletManagementContextAutoConfiguration.class,
					ServletEndpointManagementContextConfiguration.class,
					JolokiaEndpointAutoConfiguration.class, TestConfiguration.class));

	@Test
	public void jolokiaServletShouldBeEnabledByDefault() {
		this.contextRunner
				.withPropertyValues("management.endpoints.web.exposure.include=jolokia")
				.run((context) -> {
					ExposableServletEndpoint endpoint = getEndpoint(context);
					assertThat(endpoint.getRootPath()).isEqualTo("jolokia");
					Object servlet = ReflectionTestUtils
							.getField(endpoint.getEndpointServlet(), "servlet");
					assertThat(servlet).isInstanceOf(AgentServlet.class);
				});
	}

	@Test
	public void jolokiaServletWhenEndpointNotExposedShouldNotBeDiscovered() {
		this.contextRunner.run((context) -> {
			Collection<ExposableServletEndpoint> endpoints = context
					.getBean(ServletEndpointsSupplier.class).getEndpoints();
			assertThat(endpoints).isEmpty();
		});
	}

	@Test
	public void jolokiaServletWhenDisabledShouldNotBeDiscovered() {
		this.contextRunner.withPropertyValues("management.endpoint.jolokia.enabled=false")
				.withPropertyValues("management.endpoints.web.exposure.include=jolokia")
				.run((context) -> {
					Collection<ExposableServletEndpoint> endpoints = context
							.getBean(ServletEndpointsSupplier.class).getEndpoints();
					assertThat(endpoints).isEmpty();
				});
	}

	@Test
	public void jolokiaServletWhenHasCustomConfigShouldApplyInitParams() {
		this.contextRunner
				.withPropertyValues("management.endpoint.jolokia.config.debug=true")
				.withPropertyValues("management.endpoints.web.exposure.include=jolokia")
				.run((context) -> {
					ExposableServletEndpoint endpoint = getEndpoint(context);
					assertThat(endpoint.getEndpointServlet()).extracting("initParameters")
							.containsOnly(Collections.singletonMap("debug", "true"));
				});
	}

	private ExposableServletEndpoint getEndpoint(
			AssertableWebApplicationContext context) {
		Collection<ExposableServletEndpoint> endpoints = context
				.getBean(ServletEndpointsSupplier.class).getEndpoints();
		return endpoints.iterator().next();
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		public ServletEndpointDiscoverer servletEndpointDiscoverer(
				ApplicationContext applicationContext) {
			return new ServletEndpointDiscoverer(applicationContext, null,
					Collections.emptyList());
		}

	}

}
