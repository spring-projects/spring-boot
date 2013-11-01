/*
 * Copyright 2012-2013 the original author or authors.
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

import org.junit.Test;
import org.springframework.boot.TestUtils;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link SecurityAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class SecurityAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@Test
	public void testWebConfiguration() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(AuthenticationManager.class));
		// 4 for static resources, one for management endpoints and one for the rest
		assertEquals(6, this.context.getBean(FilterChainProxy.class).getFilterChains()
				.size());
	}

	@Test
	public void testDisableIgnoredStaticApplicationPaths() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "security.ignored:none");
		this.context.refresh();
		// Just the application and management endpoints now
		assertEquals(2, this.context.getBean(FilterChainProxy.class).getFilterChains()
				.size());
	}

	@Test
	public void testDisableBasicAuthOnApplicationPaths() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		TestUtils.addEnviroment(this.context, "security.basic.enabled:false");
		this.context.refresh();
		// Just the management endpoints and default ignores now
		assertEquals(5, this.context.getBean(FilterChainProxy.class).getFilterChains()
				.size());
	}

	@Test
	public void testOverrideAuthenticationManager() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class, SecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(this.context.getBean(TestConfiguration.class).authenticationManager,
				this.context.getBean(AuthenticationManager.class));
	}

	@Configuration
	protected static class TestConfiguration {

		private AuthenticationManager authenticationManager;

		@Bean
		public AuthenticationManager myAuthenticationManager() {
			this.authenticationManager = new AuthenticationManager() {

				@Override
				public Authentication authenticate(Authentication authentication)
						throws AuthenticationException {
					return new TestingAuthenticationToken("foo", "bar");
				}
			};
			return this.authenticationManager;
		}

	}

}
