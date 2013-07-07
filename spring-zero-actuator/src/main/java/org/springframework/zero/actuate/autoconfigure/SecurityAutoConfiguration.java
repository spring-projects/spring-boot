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

package org.springframework.zero.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity.IgnoredRequestConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.zero.actuate.endpoint.Endpoint;
import org.springframework.zero.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.zero.actuate.properties.SecurityProperties;
import org.springframework.zero.actuate.web.ErrorController;
import org.springframework.zero.autoconfigure.EnableAutoConfiguration;
import org.springframework.zero.context.condition.ConditionalOnClass;
import org.springframework.zero.context.condition.ConditionalOnMissingBean;
import org.springframework.zero.context.properties.EnableConfigurationProperties;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for security of a web application or
 * service. By default everything is secured with HTTP Basic authentication except the
 * {@link SecurityProperties#getIgnored() explicitly ignored} paths (defaults to
 * <code>&#47;css&#47;**, &#47;js&#47;**, &#47;images&#47;**, &#47;**&#47;favicon.ico</code>
 * ). Many aspects of the behavior can be controller with {@link SecurityProperties} via
 * externalized application properties (or via an bean definition of that type to set the
 * defaults). The user details for authentication are just placeholders
 * <code>(username=user,
 * password=password)</code> but can easily be customized by providing a bean definition
 * of type {@link AuthenticationManager}. Also provides audit logging of authentication
 * events.
 * 
 * <p>
 * The framework {@link Endpoint}s (used to expose application information to operations)
 * include a {@link Endpoint#isSensitive() sensitive} configuration option which will be
 * used as a security hint by the filter created here.
 * 
 * <p>
 * Some common simple customizations:
 * <ul>
 * <li>Switch off security completely and permanently: remove Spring Security from the
 * classpath or {@link EnableAutoConfiguration#exclude() exclude} this configuration.</li>
 * <li>Switch off security temporarily (e.g. for a dev environment): set
 * <code>security.basic.enabled: false</code></li>
 * <li>Customize the user details: add an AuthenticationManager bean</li>
 * <li>Add form login for user facing resources: add a
 * {@link WebSecurityConfigurerAdapter} and use {@link HttpSecurity#formLogin()}</li>
 * </ul>
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ EnableWebSecurity.class })
@EnableWebSecurity
@EnableConfigurationProperties
public class SecurityAutoConfiguration {

	@Bean(name = "org.springframework.zero.actuate.properties.SecurityProperties")
	@ConditionalOnMissingBean
	public SecurityProperties securityProperties() {
		return new SecurityProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public AuthenticationEventPublisher authenticationEventPublisher() {
		return new DefaultAuthenticationEventPublisher();
	}

	@Bean
	@ConditionalOnMissingBean({ BoostrapWebSecurityConfigurerAdapter.class })
	public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
		return new BoostrapWebSecurityConfigurerAdapter();
	}

	// Give user-supplied filters a chance to be last in line
	@Order(Ordered.LOWEST_PRECEDENCE - 10)
	private static class BoostrapWebSecurityConfigurerAdapter extends
			WebSecurityConfigurerAdapter {

		private static final String[] NO_PATHS = new String[0];

		@Autowired
		private SecurityProperties security;

		@Autowired(required = false)
		private EndpointHandlerMapping endpointHandlerMapping;

		@Autowired
		private AuthenticationEventPublisher authenticationEventPublisher;

		@Autowired(required = false)
		private ErrorController errorController;

		@Override
		protected void configure(HttpSecurity http) throws Exception {

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
				path = (path == null ? "" : path.trim());
				if (path.equals("/**")) {
					return new String[] { path };
				}
				if (!path.equals("")) {
					list.add(path);
				}
			}
			// FIXME makes more sense to secure endpoints with a different role
			list.addAll(Arrays.asList(getEndpointPaths(true)));
			return list.toArray(new String[list.size()]);
		}

		private AuthenticationEntryPoint entryPoint() {
			BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
			entryPoint.setRealmName(this.security.getBasic().getRealm());
			return entryPoint;
		}

		@Override
		public void configure(WebSecurity builder) throws Exception {
			IgnoredRequestConfigurer ignoring = builder.ignoring();
			ignoring.antMatchers(this.security.getIgnored());
			ignoring.antMatchers(getEndpointPaths(false));
			if (this.errorController != null) {
				ignoring.antMatchers(this.errorController.getErrorPath());
			}
		}

		private String[] getEndpointPaths(boolean secure) {
			if (this.endpointHandlerMapping == null) {
				return NO_PATHS;
			}

			// FIXME this will still open up paths on the server when a management port is
			// being used.

			List<Endpoint<?>> endpoints = this.endpointHandlerMapping.getEndpoints();
			List<String> paths = new ArrayList<String>(endpoints.size());
			for (Endpoint<?> endpoint : endpoints) {
				if (endpoint.isSensitive() == secure) {
					paths.add(endpoint.getPath());
				}
			}
			return paths.toArray(new String[paths.size()]);
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
