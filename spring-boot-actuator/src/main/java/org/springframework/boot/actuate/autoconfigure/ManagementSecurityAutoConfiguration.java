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

package org.springframework.boot.actuate.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.web.ErrorController;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.AuthenticationManagerConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityPrequisite;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.SpringBootWebSecurityConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity.IgnoredRequestConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for security of framework endpoints.
 * Many aspects of the behavior can be controller with {@link ManagementServerProperties}
 * via externalized application properties (or via an bean definition of that type to set
 * the defaults).
 * 
 * <p>
 * The framework {@link Endpoint}s (used to expose application information to operations)
 * include a {@link Endpoint#isSensitive() sensitive} configuration option which will be
 * used as a security hint by the filter created here.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ EnableWebSecurity.class })
@AutoConfigureAfter(SecurityAutoConfiguration.class)
@EnableConfigurationProperties
public class ManagementSecurityAutoConfiguration {

	private static final String[] NO_PATHS = new String[0];

	@Bean
	@ConditionalOnMissingBean({ IgnoredPathsWebSecurityConfigurerAdapter.class })
	public WebSecurityConfigurer<WebSecurity> ignoredPathsWebSecurityConfigurerAdapter() {
		return new IgnoredPathsWebSecurityConfigurerAdapter();
	}

	@Configuration
	protected static class ManagementSecurityPropertiesConfiguration implements
			SecurityPrequisite {

		@Autowired(required = false)
		private SecurityProperties security;

		@Autowired(required = false)
		private ManagementServerProperties management;

		@PostConstruct
		public void init() {
			if (this.management != null && this.security != null) {
				this.security.getUser().getRole()
						.add(this.management.getSecurity().getRole());
			}
		}

	}

	// Get the ignored paths in early
	@Order(Ordered.HIGHEST_PRECEDENCE + 1)
	private static class IgnoredPathsWebSecurityConfigurerAdapter implements
			WebSecurityConfigurer<WebSecurity> {

		@Autowired(required = false)
		private ErrorController errorController;

		@Autowired(required = false)
		private EndpointHandlerMapping endpointHandlerMapping;

		@Autowired
		private ManagementServerProperties management;

		@Autowired
		private SecurityProperties security;

		@Override
		public void configure(WebSecurity builder) throws Exception {
		}

		@Override
		public void init(WebSecurity builder) throws Exception {
			IgnoredRequestConfigurer ignoring = builder.ignoring();
			// The ignores are not cumulative, so to prevent overwriting the defaults we
			// add them back.
			List<String> ignored = SpringBootWebSecurityConfiguration
					.getIgnored(this.security);
			ignored.addAll(Arrays.asList(getEndpointPaths(this.endpointHandlerMapping,
					false)));
			if (!this.management.getSecurity().isEnabled()) {
				ignored.addAll(Arrays.asList(getEndpointPaths(
						this.endpointHandlerMapping, true)));
			}
			if (ignored.contains("none")) {
				ignored.remove("none");
			}
			if (this.errorController != null) {
				ignored.add(this.errorController.getErrorPath());
			}
			ignoring.antMatchers(ignored.toArray(new String[0]));
		}

	}

	@Configuration
	@ConditionalOnMissingBean({ ManagementWebSecurityConfigurerAdapter.class })
	@ConditionalOnExpression("${management.security.enabled:true}")
	@EnableWebSecurity
	// Give user-supplied filters a chance to be last in line
	@Order(Ordered.LOWEST_PRECEDENCE - 10)
	protected static class ManagementWebSecurityConfigurerAdapter extends
			WebSecurityConfigurerAdapter {

		@Autowired
		private SecurityProperties security;

		@Autowired
		private ManagementServerProperties management;

		@Autowired(required = false)
		private EndpointHandlerMapping endpointHandlerMapping;

		@Override
		protected void configure(HttpSecurity http) throws Exception {

			// secure endpoints
			String[] paths = getEndpointPaths(this.endpointHandlerMapping, true);
			if (paths.length > 0 && this.management.getSecurity().isEnabled()) {
				// Always protect them if present
				if (this.security.isRequireSsl()) {
					http.requiresChannel().anyRequest().requiresSecure();
				}
				http.exceptionHandling().authenticationEntryPoint(entryPoint());
				http.requestMatchers().antMatchers(paths);
				http.authorizeRequests().anyRequest()
						.hasRole(this.management.getSecurity().getRole()) //
						.and().httpBasic() //
						.and().anonymous().disable();

				// No cookies for management endpoints by default
				http.csrf().disable();
				http.sessionManagement().sessionCreationPolicy(
						this.management.getSecurity().getSessions());

				SpringBootWebSecurityConfiguration.configureHeaders(http.headers(),
						this.security.getHeaders());

			}

		}

		private AuthenticationEntryPoint entryPoint() {
			BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
			entryPoint.setRealmName(this.security.getBasic().getRealm());
			return entryPoint;
		}

		@Configuration
		@ConditionalOnMissingBean(AuthenticationManager.class)
		@Order(Ordered.LOWEST_PRECEDENCE - 4)
		protected static class ManagementAuthenticationManagerConfiguration extends
				AuthenticationManagerConfiguration {
		}

	}

	private static String[] getEndpointPaths(
			EndpointHandlerMapping endpointHandlerMapping, boolean secure) {
		if (endpointHandlerMapping == null) {
			return NO_PATHS;
		}

		Set<? extends MvcEndpoint> endpoints = endpointHandlerMapping.getEndpoints();
		List<String> paths = new ArrayList<String>(endpoints.size());
		for (MvcEndpoint endpoint : endpoints) {
			if (endpoint.isSensitive() == secure) {
				paths.add(endpoint.getPath());
			}
		}
		return paths.toArray(new String[paths.size()]);
	}

}
