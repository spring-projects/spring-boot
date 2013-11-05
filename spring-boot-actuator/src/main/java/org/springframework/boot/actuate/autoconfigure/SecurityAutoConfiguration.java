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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.Filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.properties.ManagementServerProperties;
import org.springframework.boot.actuate.properties.SecurityProperties;
import org.springframework.boot.actuate.properties.SecurityProperties.Headers;
import org.springframework.boot.actuate.properties.SecurityProperties.User;
import org.springframework.boot.actuate.web.ErrorController;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity.IgnoredRequestConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

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
// TODO: re-enable this when SPR-11069 is fixed
// @ConditionalOnMissingBean(annotation = EnableWebSecurity.class)
@ConditionalOnMissingBean(SecurityAutoConfiguration.class)
@EnableConfigurationProperties
public class SecurityAutoConfiguration {

	private static final String[] NO_PATHS = new String[0];

	@Bean(name = "org.springframework.actuate.properties.SecurityProperties")
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
	@ConditionalOnMissingBean({ ApplicationWebSecurityConfigurerAdapter.class })
	@ConditionalOnExpression("${security.basic.enabled:true}")
	public WebSecurityConfigurerAdapter applicationWebSecurityConfigurerAdapter() {
		return new ApplicationWebSecurityConfigurerAdapter();
	}

	@Bean
	@ConditionalOnMissingBean({ ManagementWebSecurityConfigurerAdapter.class })
	@ConditionalOnExpression("${security.management.enabled:true}")
	public WebSecurityConfigurerAdapter managementWebSecurityConfigurerAdapter() {
		return new ManagementWebSecurityConfigurerAdapter();
	}

	@Bean
	@ConditionalOnMissingBean({ IgnoredPathsWebSecurityConfigurerAdapter.class })
	public SecurityConfigurer<Filter, WebSecurity> ignoredPathsWebSecurityConfigurerAdapter() {
		return new IgnoredPathsWebSecurityConfigurerAdapter();
	}

	// Get the ignored paths in early
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@EnableWebSecurity
	private static class IgnoredPathsWebSecurityConfigurerAdapter implements
			SecurityConfigurer<Filter, WebSecurity> {

		private static List<String> DEFAULT_IGNORED = Arrays.asList("/css/**", "/js/**",
				"/images/**", "/**/favicon.ico");

		@Autowired(required = false)
		private ErrorController errorController;

		@Autowired(required = false)
		private EndpointHandlerMapping endpointHandlerMapping;

		@Autowired
		private SecurityProperties security;

		@Override
		public void configure(WebSecurity builder) throws Exception {
		}

		@Override
		public void init(WebSecurity builder) throws Exception {
			IgnoredRequestConfigurer ignoring = builder.ignoring();
			ignoring.antMatchers(getEndpointPaths(this.endpointHandlerMapping, false));
			List<String> ignored = new ArrayList<String>(this.security.getIgnored());
			if (!this.security.getManagement().isEnabled()) {
				ignored.addAll(Arrays.asList(getEndpointPaths(
						this.endpointHandlerMapping, true)));
			}
			if (ignored.isEmpty()) {
				ignored.addAll(DEFAULT_IGNORED);
			}
			else if (ignored.contains("none")) {
				ignored.remove("none");
			}
			if (this.errorController != null) {
				ignored.add(this.errorController.getErrorPath());
			}
			ignoring.antMatchers(ignored.toArray(new String[0]));
		}

	}

	// Give user-supplied filters a chance to be last in line
	@Order(Ordered.LOWEST_PRECEDENCE - 5)
	private static class ApplicationWebSecurityConfigurerAdapter extends
			WebSecurityConfigurerAdapter {

		@Autowired
		private SecurityProperties security;

		@Autowired
		private AuthenticationEventPublisher authenticationEventPublisher;

		@Override
		protected void configure(HttpSecurity http) throws Exception {

			if (this.security.isRequireSsl()) {
				http.requiresChannel().anyRequest().requiresSecure();
			}

			String[] paths = getSecureApplicationPaths();
			if (this.security.getBasic().isEnabled() && paths.length > 0) {
				http.exceptionHandling().authenticationEntryPoint(entryPoint());
				http.requestMatchers().antMatchers(paths);
				http.authorizeRequests().anyRequest()
						.hasRole(this.security.getUser().getRole()) //
						.and().httpBasic() //
						.and().anonymous().disable();
			}
			if (!this.security.isEnableCsrf()) {
				http.csrf().disable();
			}
			// No cookies for application endpoints by default
			http.sessionManagement().sessionCreationPolicy(this.security.getSessions());

			SecurityAutoConfiguration.configureHeaders(http.headers(),
					this.security.getHeaders());

		}

		private String[] getSecureApplicationPaths() {
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
			return list.toArray(new String[list.size()]);
		}

		private AuthenticationEntryPoint entryPoint() {
			BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
			entryPoint.setRealmName(this.security.getBasic().getRealm());
			return entryPoint;
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

	// Give user-supplied filters a chance to be last in line
	@Order(Ordered.LOWEST_PRECEDENCE - 10)
	private static class ManagementWebSecurityConfigurerAdapter extends
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
			if (paths.length > 0 && this.security.getManagement().isEnabled()) {
				// Always protect them if present
				if (this.security.isRequireSsl()) {
					http.requiresChannel().anyRequest().requiresSecure();
				}
				http.exceptionHandling().authenticationEntryPoint(entryPoint());
				http.requestMatchers().antMatchers(paths);
				http.authorizeRequests().anyRequest()
						.hasRole(this.security.getManagement().getRole()) //
						.and().httpBasic() //
						.and().anonymous().disable();

				// No cookies for management endpoints by default
				http.csrf().disable();
				http.sessionManagement().sessionCreationPolicy(
						this.security.getManagement().getSessions());

				SecurityAutoConfiguration.configureHeaders(http.headers(),
						this.security.getHeaders());

			}

		}

		private AuthenticationEntryPoint entryPoint() {
			BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
			entryPoint.setRealmName(this.security.getBasic().getRealm());
			return entryPoint;
		}

	}

	@ConditionalOnMissingBean(AuthenticationManager.class)
	@Configuration
	public static class AuthenticationManagerConfiguration {

		private static Log logger = LogFactory
				.getLog(AuthenticationManagerConfiguration.class);

		@Autowired
		private SecurityProperties security;

		@Bean
		public AuthenticationManager authenticationManager(
				ObjectPostProcessor<Object> objectPostProcessor) throws Exception {

			InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> builder = new AuthenticationManagerBuilder(
					objectPostProcessor).inMemoryAuthentication();
			User user = this.security.getUser();

			if (user.isDefaultPassword()) {
				logger.info("\n\n" + "Using default password for application endpoints: "
						+ user.getPassword() + "\n\n");
			}

			Set<String> roles = new LinkedHashSet<String>(Arrays.asList(this.security
					.getManagement().getRole(), user.getRole()));

			builder.withUser(user.getName()).password(user.getPassword())
					.roles(roles.toArray(new String[roles.size()]));

			return builder.and().build();

		}

	}

	private static String[] getEndpointPaths(
			EndpointHandlerMapping endpointHandlerMapping, boolean secure) {
		if (endpointHandlerMapping == null) {
			return NO_PATHS;
		}

		List<Endpoint<?>> endpoints = endpointHandlerMapping.getEndpoints();
		List<String> paths = new ArrayList<String>(endpoints.size());
		for (Endpoint<?> endpoint : endpoints) {
			if (endpoint.isSensitive() == secure) {
				paths.add(endpoint.getPath());
			}
		}
		return paths.toArray(new String[paths.size()]);
	}

	private static void configureHeaders(HeadersConfigurer<?> configurer,
			SecurityProperties.Headers headers) throws Exception {
		if (headers.getHsts() != Headers.HSTS.none) {
			boolean includeSubdomains = headers.getHsts() == Headers.HSTS.all;
			HstsHeaderWriter writer = new HstsHeaderWriter(includeSubdomains);
			writer.setRequestMatcher(AnyRequestMatcher.INSTANCE);
			configurer.addHeaderWriter(writer);
		}
		if (headers.isContentType()) {
			configurer.contentTypeOptions();
		}
		if (headers.isXss()) {
			configurer.xssProtection();
		}
		if (headers.isCache()) {
			configurer.cacheControl();
		}
		if (headers.isFrame()) {
			configurer.frameOptions();
		}
	}

}
