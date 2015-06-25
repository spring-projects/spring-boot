/*
 * Copyright 2012-2015 the original author or authors.
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

import javax.servlet.Filter;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.EnableGlobalAuthentication;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link ManagementSecurityAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class ManagementSecurityAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testWebConfiguration() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				ManagementSecurityAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "security.basic.enabled:false");
		this.context.refresh();
		assertNotNull(this.context.getBean(AuthenticationManagerBuilder.class));
		FilterChainProxy filterChainProxy = this.context.getBean(FilterChainProxy.class);
		// 4 for static resources, one for management endpoints and one for the rest
		assertThat(filterChainProxy.getFilterChains(), hasSize(6));
		assertThat(filterChainProxy.getFilters("/beans"), hasSize(greaterThan(0)));
		assertThat(filterChainProxy.getFilters("/beans/"), hasSize(greaterThan(0)));
		assertThat(filterChainProxy.getFilters("/beans.foo"), hasSize(greaterThan(0)));
		assertThat(filterChainProxy.getFilters("/beans/foo/bar"), hasSize(greaterThan(0)));
	}

	@Test
	public void testPathNormalization() throws Exception {
		String path = "admin/./error";
		assertEquals("admin/error", StringUtils.cleanPath(path));
	}

	@Test
	public void testWebConfigurationWithExtraRole() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(EndpointAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				SecurityAutoConfiguration.class,
				ManagementSecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		UserDetails user = getUser();
		assertTrue(user.getAuthorities().containsAll(
				AuthorityUtils
						.commaSeparatedStringToAuthorityList("ROLE_USER,ROLE_ADMIN")));
	}

	private UserDetails getUser() {
		ProviderManager parent = (ProviderManager) this.context
				.getBean(AuthenticationManager.class);
		DaoAuthenticationProvider provider = (DaoAuthenticationProvider) parent
				.getProviders().get(0);
		UserDetailsService service = (UserDetailsService) ReflectionTestUtils.getField(
				provider, "userDetailsService");
		UserDetails user = service.loadUserByUsername("user");
		return user;
	}

	@Test
	public void testDisableIgnoredStaticApplicationPaths() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				ManagementSecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "security.ignored:none");
		this.context.refresh();
		// Just the application and management endpoints now
		assertEquals(2, this.context.getBean(FilterChainProxy.class).getFilterChains()
				.size());
	}

	@Test
	public void testDisableBasicAuthOnApplicationPaths() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(HttpMessageConvertersAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				SecurityAutoConfiguration.class,
				ManagementSecurityAutoConfiguration.class,
				FallbackWebSecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "security.basic.enabled:false");
		this.context.refresh();
		// Just the management endpoints (one filter) and ignores now plus the backup
		// filter on app endpoints
		assertEquals(6, this.context.getBean(FilterChainProxy.class).getFilterChains()
				.size());
	}

	@Test
	public void testOverrideAuthenticationManager() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class, SecurityAutoConfiguration.class,
				ManagementSecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(this.context.getBean(TestConfiguration.class).authenticationManager,
				this.context.getBean(AuthenticationManager.class));
	}

	@Test
	public void testSecurityPropertiesNotAvailable() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class, SecurityAutoConfiguration.class,
				ManagementSecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(this.context.getBean(TestConfiguration.class).authenticationManager,
				this.context.getBean(AuthenticationManager.class));
	}

	// gh-2466
	@Test
	public void realmSameForManagement() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(AuthenticationConfig.class,
				SecurityAutoConfiguration.class,
				ManagementSecurityAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();

		Filter filter = this.context.getBean("springSecurityFilterChain", Filter.class);
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.addFilters(filter).build();

		// no user (Main)
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(springAuthenticateRealmHeader());

		// invalid user (Main)
		mockMvc.perform(
				MockMvcRequestBuilders.get("/").header("authorization", "Basic xxx"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(springAuthenticateRealmHeader());

		// no user (Management)
		mockMvc.perform(MockMvcRequestBuilders.get("/beans"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(springAuthenticateRealmHeader());

		// invalid user (Management)
		mockMvc.perform(
				MockMvcRequestBuilders.get("/beans").header("authorization", "Basic xxx"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(springAuthenticateRealmHeader());
	}

	private ResultMatcher springAuthenticateRealmHeader() {
		return MockMvcResultMatchers.header().string("www-authenticate",
				Matchers.containsString("realm=\"Spring\""));
	}

	@EnableGlobalAuthentication
	@Configuration
	static class AuthenticationConfig {
		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			auth.inMemoryAuthentication().withUser("user").password("password")
					.roles("USER");
		}
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
