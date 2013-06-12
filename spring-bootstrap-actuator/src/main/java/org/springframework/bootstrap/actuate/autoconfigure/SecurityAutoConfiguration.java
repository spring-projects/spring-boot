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

package org.springframework.bootstrap.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.actuate.properties.EndpointsProperties;
import org.springframework.bootstrap.actuate.properties.SecurityProperties;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.EnableWebSecurity;
import org.springframework.security.config.annotation.web.HttpConfiguration;
import org.springframework.security.config.annotation.web.WebSecurityBuilder;
import org.springframework.security.config.annotation.web.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * <p>
 * Auto configuration for security of a web application or service. By default everything
 * is secured with HTTP Basic authentication except the
 * {@link SecurityProperties#getIgnored() explicitly ignored} paths (defaults to
 * <code>/css&#47;**, /js&#47;**, /images&#47;**, &#47;**&#47;favicon.ico</code>). Many
 * aspects of the behaviour can be controller with {@link SecurityProperties} via
 * externalized application properties (or via an bean definition of that type to set the
 * defaults). The user details for authentication are just placeholders
 * <code>(username=user,
 * password=password)</code> but can easily be customized by providing a bean definition
 * of type {@link AuthenticationManager}. Also provides audit logging of authentication
 * events.
 * </p>
 * 
 * <p>
 * The framework {@link EndpointsProperties} configuration bean has explicitly
 * {@link EndpointsProperties#getSecurePaths() secure} and
 * {@link EndpointsProperties#getOpenPaths() open} paths (by name) which are always
 * respected by the filter created here. You can override the paths of those endpoints
 * using application properties (e.g. <code>endpoints.info.path</code> is open, and
 * <code>endpoints.metrics.path</code> is secure), but not the security aspects. The
 * always secure paths are management endpoints that would be inadvisable to expose to all
 * users.
 * </p>
 * 
 * <p>
 * Some common simple customizations:
 * <ul>
 * <li>Switch off security completely and permanently: remove Spring Security from the
 * classpath</li>
 * <li>Switch off security temporarily (e.g. for a dev environment): set
 * <code>security.basic.enabled: false</code></li>
 * <li>Customize the user details: add an AuthenticationManager bean</li>
 * <li>Add form login for user facing resources: add a
 * {@link WebSecurityConfigurerAdapter} and use {@link HttpConfiguration#formLogin()}</li>
 * </ul>
 * </p>
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ EnableWebSecurity.class })
@EnableWebSecurity
@EnableConfigurationProperties
public class SecurityAutoConfiguration {

	@ConditionalOnMissingBean(SecurityProperties.class)
	@Bean(name = "org.springframework.bootstrap.actuate.properties.SecurityProperties")
	public SecurityProperties securityProperties() {
		return new SecurityProperties();
	}

	@Bean
	@ConditionalOnMissingBean({ AuthenticationEventPublisher.class })
	public AuthenticationEventPublisher authenticationEventPublisher() {
		return new DefaultAuthenticationEventPublisher();
	}

	@Bean
	@ConditionalOnMissingBean({ BoostrapWebSecurityConfigurerAdapter.class })
	public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
		return new BoostrapWebSecurityConfigurerAdapter();
	}

	// Give user-supplied filters a chance to be last in line
	@Order(Integer.MAX_VALUE - 10)
	private static class BoostrapWebSecurityConfigurerAdapter extends
			WebSecurityConfigurerAdapter {

		@Autowired
		private SecurityProperties security;

		@Autowired
		private EndpointsProperties endpoints;

		@Autowired
		private AuthenticationEventPublisher authenticationEventPublisher;

		@Override
		protected void configure(HttpConfiguration http) throws Exception {

			if (this.security.isRequireSsl()) {
				http.requiresChannel().anyRequest().requiresSecure();
			}

			if (this.security.getBasic().isEnabled()) {

				String[] paths = getSecurePaths();
				http.exceptionHandling().authenticationEntryPoint(entryPoint()).and()
						.requestMatchers().antMatchers(paths);
				http.httpBasic().and().anonymous().disable();
				http.authorizeUrls().anyRequest()
						.hasRole(this.security.getBasic().getRole());

			}

			// No cookies for service endpoints by default
			http.sessionManagement().sessionCreationPolicy(this.security.getSessions());

		}

		private String[] getSecurePaths() {
			List<String> list = new ArrayList<String>();
			for (String path : this.security.getBasic().getPath()) {
				path = path == null ? "" : path.trim();
				if (path.equals("/**")) {
					return new String[] { path };
				}
				if (!path.equals("")) {
					list.add(path);
				}
			}
			list.addAll(Arrays.asList(this.endpoints.getSecurePaths()));
			return list.toArray(new String[list.size()]);
		}

		private AuthenticationEntryPoint entryPoint() {
			BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
			entryPoint.setRealmName(this.security.getBasic().getRealm());
			return entryPoint;
		}

		@Override
		public void configure(WebSecurityBuilder builder) throws Exception {
			builder.ignoring().antMatchers(this.security.getIgnored())
					.antMatchers(this.endpoints.getOpenPaths());
		}

		@Override
		protected AuthenticationManager authenticationManager() throws Exception {
			AuthenticationManager manager = super.authenticationManager();
			if (manager instanceof ProviderManager) {
				((ProviderManager) manager)
						.setAuthenticationEventPublisher(this.authenticationEventPublisher);
			}
			return manager;
		}

	}

	@ConditionalOnMissingBean(AuthenticationManager.class)
	@Configuration
	public static class AuthenticationManagerConfiguration {

		@Bean
		public AuthenticationManager authenticationManager() throws Exception {
			return new AuthenticationManagerBuilder().inMemoryAuthentication()
					.withUser("user").password("password").roles("USER").and().and()
					.build();
		}

	}

}
