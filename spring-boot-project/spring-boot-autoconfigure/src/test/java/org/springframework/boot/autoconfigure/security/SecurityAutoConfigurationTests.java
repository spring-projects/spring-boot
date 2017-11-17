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

package org.springframework.boot.autoconfigure.security;

import java.util.Collections;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.data.repository.query.SecurityEvaluationContextExtension;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Rob Winch
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
public class SecurityAutoConfigurationTests {

	private AnnotationConfigWebApplicationContext context;

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

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
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(AuthenticationManagerBuilder.class)).isNotNull();
		assertThat(this.context.getBean(FilterChainProxy.class).getFilterChains())
				.hasSize(1);
	}

	@Test
	public void testDefaultFilterOrderWithSecurityAdapter() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(WebSecurity.class, SecurityAutoConfiguration.class,
				SecurityFilterAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean("securityFilterChainRegistration",
				DelegatingFilterProxyRegistrationBean.class).getOrder()).isEqualTo(
						FilterRegistrationBean.REQUEST_WRAPPER_FILTER_MAX_ORDER - 100);
	}

	@Test
	public void testFilterIsNotRegisteredInNonWeb() throws Exception {
		try (AnnotationConfigApplicationContext customContext = new AnnotationConfigApplicationContext()) {
			customContext.register(SecurityAutoConfiguration.class,
					SecurityFilterAutoConfiguration.class,
					PropertyPlaceholderAutoConfiguration.class);
			customContext.refresh();
			assertThat(customContext.containsBean("securityFilterChainRegistration"))
					.isFalse();
		}
	}

	@Test
	public void testDefaultFilterOrder() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				SecurityFilterAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean("securityFilterChainRegistration",
				DelegatingFilterProxyRegistrationBean.class).getOrder()).isEqualTo(
						FilterRegistrationBean.REQUEST_WRAPPER_FILTER_MAX_ORDER - 100);
	}

	@Test
	public void testCustomFilterOrder() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		TestPropertyValues.of("spring.security.filter.order:12345").applyTo(this.context);
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				SecurityFilterAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean("securityFilterChainRegistration",
				DelegatingFilterProxyRegistrationBean.class).getOrder()).isEqualTo(12345);
	}

	@Test
	public void testDefaultUsernamePassword() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class);
		this.context.refresh();
		UserDetailsService manager = this.context.getBean(UserDetailsService.class);
		assertThat(this.outputCapture.toString())
				.contains("Using default security password:");
		assertThat(manager.loadUserByUsername("user")).isNotNull();
	}

	@Test
	public void defaultUserNotCreatedIfAuthenticationManagerBeanPresent()
			throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestAuthenticationManagerConfiguration.class,
				SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		AuthenticationManager manager = this.context.getBean(AuthenticationManager.class);
		assertThat(manager).isEqualTo(this.context.getBean(
				TestAuthenticationManagerConfiguration.class).authenticationManager);
		assertThat(this.outputCapture.toString())
				.doesNotContain("Using default security password: ");
		TestingAuthenticationToken token = new TestingAuthenticationToken("foo", "bar");
		assertThat(manager.authenticate(token)).isNotNull();
	}

	@Test
	public void defaultUserNotCreatedIfUserDetailsServiceBeanPresent() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestUserDetailsServiceConfiguration.class,
				SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		UserDetailsService userDetailsService = this.context
				.getBean(UserDetailsService.class);
		assertThat(this.outputCapture.toString())
				.doesNotContain("Using default security password: ");
		assertThat(userDetailsService.loadUserByUsername("foo")).isNotNull();
	}

	@Test
	public void defaultUserNotCreatedIfAuthenticationProviderBeanPresent()
			throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestAuthenticationProviderConfiguration.class,
				SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		AuthenticationProvider provider = this.context
				.getBean(AuthenticationProvider.class);
		assertThat(this.outputCapture.toString())
				.doesNotContain("Using default security password: ");
		TestingAuthenticationToken token = new TestingAuthenticationToken("foo", "bar");
		assertThat(provider.authenticate(token)).isNotNull();
	}

	@Test
	public void testJpaCoexistsHappily() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		TestPropertyValues
				.of("spring.datasource.url:jdbc:hsqldb:mem:testsecdb",
						"spring.datasource.initialization-mode:never")
				.applyTo(this.context);
		this.context.register(EntityConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
				SecurityAutoConfiguration.class);
		// This can fail if security @Conditionals force early instantiation of the
		// HibernateJpaAutoConfiguration (e.g. the EntityManagerFactory is not found)
		this.context.refresh();
		assertThat(this.context.getBean(JpaTransactionManager.class)).isNotNull();
	}

	@Test
	public void testSecurityEvaluationContextExtensionSupport() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(SecurityEvaluationContextExtension.class))
				.isNotNull();
	}

	@Test
	public void defaultFilterDispatcherTypes() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				SecurityFilterAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DelegatingFilterProxyRegistrationBean bean = this.context.getBean(
				"securityFilterChainRegistration",
				DelegatingFilterProxyRegistrationBean.class);
		@SuppressWarnings("unchecked")
		EnumSet<DispatcherType> dispatcherTypes = (EnumSet<DispatcherType>) ReflectionTestUtils
				.getField(bean, "dispatcherTypes");
		assertThat(dispatcherTypes).containsOnly(DispatcherType.ASYNC,
				DispatcherType.ERROR, DispatcherType.REQUEST);
	}

	@Test
	public void customFilterDispatcherTypes() {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				SecurityFilterAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		TestPropertyValues.of("spring.security.filter.dispatcher-types:INCLUDE,ERROR")
				.applyTo(this.context);
		this.context.refresh();
		DelegatingFilterProxyRegistrationBean bean = this.context.getBean(
				"securityFilterChainRegistration",
				DelegatingFilterProxyRegistrationBean.class);
		@SuppressWarnings("unchecked")
		EnumSet<DispatcherType> dispatcherTypes = (EnumSet<DispatcherType>) ReflectionTestUtils
				.getField(bean, "dispatcherTypes");
		assertThat(dispatcherTypes).containsOnly(DispatcherType.INCLUDE,
				DispatcherType.ERROR);
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class EntityConfiguration {

	}

	@Configuration
	protected static class TestAuthenticationManagerConfiguration {

		private AuthenticationManager authenticationManager;

		@Bean
		public AuthenticationManager myAuthenticationManager() {
			AuthenticationProvider authenticationProvider = new TestingAuthenticationProvider();
			this.authenticationManager = new ProviderManager(
					Collections.singletonList(authenticationProvider));
			return this.authenticationManager;
		}

	}

	@Configuration
	protected static class TestUserDetailsServiceConfiguration {

		@Bean
		public InMemoryUserDetailsManager myUserDetailsService() {
			return new InMemoryUserDetailsManager(
					User.withUsername("foo").password("bar").roles("USER").build());
		}

	}

	@Configuration
	protected static class TestAuthenticationProviderConfiguration {

		@Bean
		public AuthenticationProvider myAuthenticationProvider() {
			return new TestingAuthenticationProvider();
		}

	}

	@Configuration
	@EnableWebSecurity
	static class WebSecurity extends WebSecurityConfigurerAdapter {

	}

}
