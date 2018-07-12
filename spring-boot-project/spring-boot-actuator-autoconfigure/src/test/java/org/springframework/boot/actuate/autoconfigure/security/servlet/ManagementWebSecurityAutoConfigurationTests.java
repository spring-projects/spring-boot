/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.actuate.autoconfigure.security.servlet;

import java.io.IOException;

import org.junit.Test;
import org.testcontainers.shaded.org.apache.http.HttpStatus;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementWebSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class ManagementWebSecurityAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					HealthIndicatorAutoConfiguration.class,
					HealthEndpointAutoConfiguration.class,
					InfoEndpointAutoConfiguration.class,
					EnvironmentEndpointAutoConfiguration.class,
					EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					SecurityAutoConfiguration.class,
					ManagementWebSecurityAutoConfiguration.class));

	@Test
	public void permitAllForHealth() {
		this.contextRunner.run((context) -> {
			int status = getResponseStatus(context, "/actuator/health");
			assertThat(status).isEqualTo(HttpStatus.SC_OK);
		});
	}

	@Test
	public void permitAllForInfo() {
		this.contextRunner.run((context) -> {
			int status = getResponseStatus(context, "/actuator/info");
			assertThat(status).isEqualTo(HttpStatus.SC_OK);
		});
	}

	@Test
	public void securesEverythingElse() {
		this.contextRunner.run((context) -> {
			int status = getResponseStatus(context, "/actuator");
			assertThat(status).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
			status = getResponseStatus(context, "/foo");
			assertThat(status).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
		});
	}

	@Test
	public void usesMatchersBasedOffConfiguredActuatorBasePath() {
		this.contextRunner.withPropertyValues("management.endpoints.web.base-path=/")
				.run((context) -> {
					int status = getResponseStatus(context, "/health");
					assertThat(status).isEqualTo(HttpStatus.SC_OK);
				});
	}

	@Test
	public void backOffIfCustomSecurityIsAdded() {
		this.contextRunner.withUserConfiguration(CustomSecurityConfiguration.class)
				.run((context) -> {
					int status = getResponseStatus(context, "/actuator/health");
					assertThat(status).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
					status = getResponseStatus(context, "/foo");
					assertThat(status).isEqualTo(HttpStatus.SC_OK);
				});
	}

	private int getResponseStatus(AssertableWebApplicationContext context, String path)
			throws IOException, javax.servlet.ServletException {
		FilterChainProxy filterChainProxy = context.getBean(FilterChainProxy.class);
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletResponse response = new MockHttpServletResponse();
		servletContext.setAttribute(
				WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setServletPath(path);
		request.setMethod("GET");
		filterChainProxy.doFilter(request, response, new MockFilterChain());
		return response.getStatus();
	}

	@Configuration
	static class CustomSecurityConfiguration extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().antMatchers("/foo").permitAll().anyRequest()
					.authenticated().and().formLogin().and().httpBasic();
		}

	}

}
