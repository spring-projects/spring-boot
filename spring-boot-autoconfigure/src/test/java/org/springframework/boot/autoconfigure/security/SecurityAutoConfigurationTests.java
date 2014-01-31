/*
 * Copyright 2012-2014 the original author or authors.
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

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationReportLoggingInitializer;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.context.listener.LoggingApplicationListener;
import org.springframework.boot.event.ApplicationPreparedEvent;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
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
				PropertyPlaceholderAutoConfiguration.class);
		debugRefresh(this.context);
		assertNotNull(this.context.getBean(AuthenticationManagerBuilder.class));
		// 4 for static resources and one for the rest
		assertEquals(5, this.context.getBean(FilterChainProxy.class).getFilterChains()
				.size());
	}

	@Test
	public void testDisableIgnoredStaticApplicationPaths() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "security.ignored:none");
		this.context.refresh();
		// Just the application endpoints now
		assertEquals(1, this.context.getBean(FilterChainProxy.class).getFilterChains()
				.size());
	}

	@Test
	public void testDisableBasicAuthOnApplicationPaths() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "security.basic.enabled:false");
		this.context.refresh();
		// No security at all not even ignores
		assertEquals(0, this.context.getBeanNamesForType(FilterChainProxy.class).length);
	}

	@Test
	public void testOverrideAuthenticationManager() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(TestConfiguration.class, SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(this.context.getBean(TestConfiguration.class).authenticationManager,
				this.context.getBean(AuthenticationManager.class));
	}

	@Test
	public void testJpaCoexistsHappily() throws Exception {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.setServletContext(new MockServletContext());
		this.context.register(EntityConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
				SecurityAutoConfiguration.class);
		// This can fail if security @Conditionals force early instantiation of the
		// HibernateJpaAutoConfiguration (e.g. the EntityManagerFactory is not found)
		this.context.refresh();
		assertNotNull(this.context.getBean(JpaTransactionManager.class));
	}

	private static AnnotationConfigWebApplicationContext debugRefresh(
			AnnotationConfigWebApplicationContext context) {
		EnvironmentTestUtils.addEnvironment(context, "debug:true");
		LoggingApplicationListener logging = new LoggingApplicationListener();
		logging.onApplicationEvent(new ApplicationPreparedEvent(new SpringApplication(),
				new String[0], context));
		AutoConfigurationReportLoggingInitializer initializer = new AutoConfigurationReportLoggingInitializer();
		initializer.initialize(context);
		context.refresh();
		initializer.onApplicationEvent(new ContextRefreshedEvent(context));
		return context;
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class EntityConfiguration {

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
