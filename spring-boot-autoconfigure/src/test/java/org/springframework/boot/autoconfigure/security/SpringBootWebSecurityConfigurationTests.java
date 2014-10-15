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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Tests for {@link SpringBootWebSecurityConfiguration}.
 *
 * @author Dave Syer
 */
public class SpringBootWebSecurityConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testDefaultIgnores() {
		assertTrue(SpringBootWebSecurityConfiguration
				.getIgnored(new SecurityProperties()).contains("/css/**"));
	}

	@Test
	public void testWebConfigurationOverrideGlobalAuthentication() throws Exception {
		this.context = SpringApplication.run(TestWebConfiguration.class,
				"--server.port=0", "--debug");
		assertNotNull(this.context.getBean(AuthenticationManagerBuilder.class));
		assertNotNull(this.context.getBean(AuthenticationManager.class).authenticate(
				new UsernamePasswordAuthenticationToken("dave", "secret")));
	}

	@Test
	public void testWebConfigurationInjectGlobalAuthentication() throws Exception {
		this.context = SpringApplication.run(TestInjectWebConfiguration.class,
				"--server.port=0", "--debug");
		assertNotNull(this.context.getBean(AuthenticationManagerBuilder.class));
		assertNotNull(this.context.getBean(AuthenticationManager.class).authenticate(
				new UsernamePasswordAuthenticationToken("dave", "secret")));
	}

	@Configuration
	@Import(TestWebConfiguration.class)
	@Order(Ordered.LOWEST_PRECEDENCE)
	protected static class TestInjectWebConfiguration extends
			WebSecurityConfigurerAdapter {

		// It's a bad idea to inject an AuthenticationManager into a
		// WebSecurityConfigurerAdapter because it can cascade early instantiation, 
		// unless you explicitly want the Boot default AuthenticationManager. It's
		// better to inject the builder, if you want the global AuthenticationManager. It
		// might even be necessary to wrap the builder in a lazy AuthenticationManager
		// (that calls getOrBuild() only when the AuthenticationManager is actually
		// called).
		@Autowired
		private AuthenticationManagerBuilder auth;

		@Override
		public void init(WebSecurity web) throws Exception {
			auth.getOrBuild();
		}
	}

	@MinimalWebConfiguration
	@Import(SecurityAutoConfiguration.class)
	@Order(Ordered.HIGHEST_PRECEDENCE + 10)
	protected static class TestWebConfiguration extends WebSecurityConfigurerAdapter {

		@Autowired
		public void init(AuthenticationManagerBuilder auth) throws Exception {
			// @formatter:off
			auth.inMemoryAuthentication()
				.withUser("dave")
				.password("secret")
				.roles("USER");
			// @formatter:on
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().anyRequest().denyAll();
		}

	}

	@Configuration
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			ErrorMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	protected static @interface MinimalWebConfiguration {
	}

}
