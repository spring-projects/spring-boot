/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.ArrayList;

import javax.servlet.Filter;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.FallbackWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
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
import org.springframework.security.core.GrantedAuthority;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ManagementWebSecurityAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
public class ManagementWebSecurityAutoConfigurationTests {

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
				WebMvcAutoConfiguration.class,
				ManagementWebSecurityAutoConfiguration.class,
				JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, AuditAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "security.basic.enabled:false");
		this.context.refresh();
		assertThat(this.context.getBean(AuthenticationManagerBuilder.class)).isNotNull();
		FilterChainProxy filterChainProxy = this.context.getBean(FilterChainProxy.class);
		// 1 for static resources, one for management endpoints and one for the rest
		assertThat(filterChainProxy.getFilterChains()).hasSize(3);
		assertThat(filterChainProxy.getFilters("/beans")).isNotEmpty();
		assertThat(filterChainProxy.getFilters("/beans/")).isNotEmpty();
		assertThat(filterChainProxy.getFilters("/beans.foo")).isNotEmpty();
		assertThat(filterChainProxy.getFilters("/beans/foo/bar")).isNotEmpty();
	}

	@Test
	public void testPathNormalization() throws Exception {
		String path = "admin/./error";
		assertThat(StringUtils.cleanPath(path)).isEqualTo("admin/error");
	}

	@Test
	public void testWebConfigurationWithExtraRole() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(WebConfiguration.class);
		this.context.refresh();
		UserDetails user = getUser();
		ArrayList<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>(
				user.getAuthorities());
		assertThat(authorities).containsAll(AuthorityUtils
				.commaSeparatedStringToAuthorityList("ROLE_USER,ROLE_ACTUATOR"));
	}

	private UserDetails getUser() {
		ProviderManager parent = (ProviderManager) this.context
				.getBean(AuthenticationManager.class);
		DaoAuthenticationProvider provider = (DaoAuthenticationProvider) parent
				.getProviders().get(0);
		UserDetailsService service = (UserDetailsService) ReflectionTestUtils
				.getField(provider, "userDetailsService");
		UserDetails user = service.loadUserByUsername("user");
		return user;
	}

	@Test
	public void testDisableIgnoredStaticApplicationPaths() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				ManagementWebSecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "security.ignored:none");
		this.context.refresh();
		// Just the application and management endpoints now
		assertThat(this.context.getBean(FilterChainProxy.class).getFilterChains())
				.hasSize(2);
	}

	@Test
	public void testDisableBasicAuthOnApplicationPaths() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(WebConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "security.basic.enabled:false");
		this.context.refresh();
		// Just the management endpoints (one filter) and ignores now plus the backup
		// filter on app endpoints
		assertThat(this.context.getBean(FilterChainProxy.class).getFilterChains())
				.hasSize(3);
	}

	@Test
	public void testOverrideAuthenticationManager() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class, SecurityAutoConfiguration.class,
				ManagementWebSecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(AuthenticationManager.class)).isEqualTo(
				this.context.getBean(TestConfiguration.class).authenticationManager);
	}

	@Test
	public void testSecurityPropertiesNotAvailable() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class, SecurityAutoConfiguration.class,
				ManagementWebSecurityAutoConfiguration.class,
				EndpointAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(AuthenticationManager.class)).isEqualTo(
				this.context.getBean(TestConfiguration.class).authenticationManager);
	}

	// gh-2466
	@Test
	public void realmSameForManagement() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(AuthenticationConfig.class, SecurityAutoConfiguration.class,
				ManagementWebSecurityAutoConfiguration.class,
				JacksonAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				WebMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
				AuditAutoConfiguration.class);
		this.context.refresh();

		Filter filter = this.context.getBean("springSecurityFilterChain", Filter.class);
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.addFilters(filter).build();

		// no user (Main)
		mockMvc.perform(MockMvcRequestBuilders.get("/home"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(springAuthenticateRealmHeader());

		// invalid user (Main)
		mockMvc.perform(
				MockMvcRequestBuilders.get("/home").header("authorization", "Basic xxx"))
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

	@Test
	public void testMarkAllEndpointsSensitive() throws Exception {
		// gh-4368
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(WebConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "endpoints.sensitive:true");
		this.context.refresh();

		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context) //
				.apply(springSecurity()) //
				.build();

		mockMvc //
				.perform(get("/health")) //
				.andExpect(status().isUnauthorized());
		mockMvc //
				.perform(get("/info")) //
				.andExpect(status().isUnauthorized());
	}

	private ResultMatcher springAuthenticateRealmHeader() {
		return MockMvcResultMatchers.header().string("www-authenticate",
				Matchers.containsString("realm=\"Spring\""));
	}

	@Configuration
	@ImportAutoConfiguration({ SecurityAutoConfiguration.class,
			WebMvcAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class,
			JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			EndpointAutoConfiguration.class, EndpointWebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, AuditAutoConfiguration.class,
			FallbackWebSecurityAutoConfiguration.class })
	static class WebConfiguration {

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
