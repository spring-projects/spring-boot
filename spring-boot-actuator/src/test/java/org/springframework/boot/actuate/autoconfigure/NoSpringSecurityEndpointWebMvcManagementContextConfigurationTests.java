/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpointSecurityInterceptor;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebClientAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.junit.runner.classpath.ClassPathExclusions;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointWebMvcManagementContextConfiguration} when Spring Security is not available.
 *
 * @author Madhura Bhave
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("spring-security-*.jar")
public class NoSpringSecurityEndpointWebMvcManagementContextConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@Before
	public void setup() {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				WebClientAutoConfiguration.class,
				EndpointWebMvcManagementContextConfiguration.class);
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void endpointHandlerMappingWhenSecurityEnabledShouldHaveSecurityInterceptor() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.security.enabled=true",
				"management.security.roles=my-role,your-role");
		this.context.refresh();
		EndpointHandlerMapping mapping = this.context.getBean("endpointHandlerMapping",
				EndpointHandlerMapping.class);
		MvcEndpointSecurityInterceptor securityInterceptor = (MvcEndpointSecurityInterceptor) ReflectionTestUtils
				.getField(mapping, "securityInterceptor");
		List<String> roles = getRoles(securityInterceptor);
		assertThat(roles).containsExactly("my-role", "your-role");
	}

	@Test
	public void endpointHandlerMappingWhenSecurityDisabledShouldHaveSecurityInterceptor() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"management.security.enabled=false",
				"management.security.roles=my-role,your-role");
		this.context.refresh();
		EndpointHandlerMapping mapping = this.context.getBean("endpointHandlerMapping",
				EndpointHandlerMapping.class);
		MvcEndpointSecurityInterceptor securityInterceptor = (MvcEndpointSecurityInterceptor) ReflectionTestUtils
				.getField(mapping, "securityInterceptor");
		assertThat(securityInterceptor).isNull();
	}

	@SuppressWarnings("unchecked")
	private List<String> getRoles(MvcEndpointSecurityInterceptor securityInterceptor) {
		return (List<String>) ReflectionTestUtils.getField(securityInterceptor, "roles");
	}

}

