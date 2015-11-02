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

package org.springframework.boot.autoconfigure.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties.Headers;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for security of a web application or
 * service. By default everything is secured with HTTP Basic authentication except the
 * {@link SecurityProperties#getIgnored() explicitly ignored} paths (defaults to
 * <code>&#47;css&#47;**, &#47;js&#47;**, &#47;images&#47;**, &#47;**&#47;favicon.ico</code>
 * ). Many aspects of the behavior can be controller with {@link SecurityProperties} via
 * externalized application properties (or via an bean definition of that type to set the
 * defaults). The user details for authentication are just placeholders
 * {@code (username=user, password=password)} but can easily be customized by providing a
 * an {@link AuthenticationManager}. Also provides audit logging of authentication events.
 * <p>
 * Some common simple customizations:
 * <ul>
 * <li>Switch off security completely and permanently: remove Spring Security from the
 * classpath or {@link EnableAutoConfiguration#exclude() exclude} this configuration.</li>
 * <li>Switch off security temporarily (e.g. for a dev environment): set
 * {@code security.basic.enabled: false}</li>
 * <li>Customize the user details: autowire an {@link AuthenticationManagerBuilder} into a
 * method in one of your configuration classes or equivalently add a bean of type
 * AuthenticationManager</li>
 * <li>Add form login for user facing resources: add a
 * {@link WebSecurityConfigurerAdapter} and use {@link HttpSecurity#formLogin()}</li>
 * </ul>
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass({ EnableWebSecurity.class, AuthenticationEntryPoint.class })
@ConditionalOnMissingBean(WebSecurityConfiguration.class)
@ConditionalOnWebApplication
@EnableWebSecurity
public class SpringBootWebSecurityConfiguration {

	private static List<String> DEFAULT_IGNORED = Arrays.asList("/css/**", "/js/**",
			"/images/**", "/**/favicon.ico");

	@Bean
	@ConditionalOnMissingBean({ IgnoredPathsWebSecurityConfigurerAdapter.class })
	public IgnoredPathsWebSecurityConfigurerAdapter ignoredPathsWebSecurityConfigurerAdapter() {
		return new IgnoredPathsWebSecurityConfigurerAdapter();
	}

	public static void configureHeaders(HeadersConfigurer<?> configurer,
			SecurityProperties.Headers headers) throws Exception {
		if (headers.getHsts() != Headers.HSTS.NONE) {
			boolean includeSubdomains = headers.getHsts() == Headers.HSTS.ALL;
			HstsHeaderWriter writer = new HstsHeaderWriter(includeSubdomains);
			writer.setRequestMatcher(AnyRequestMatcher.INSTANCE);
			configurer.addHeaderWriter(writer);
		}
		if (!headers.isContentType()) {
			configurer.contentTypeOptions().disable();
		}
		if (!headers.isXss()) {
			configurer.xssProtection().disable();
		}
		if (!headers.isCache()) {
			configurer.cacheControl().disable();
		}
		if (!headers.isFrame()) {
			configurer.frameOptions().disable();
		}
	}

	public static List<String> getIgnored(SecurityProperties security) {
		List<String> ignored = new ArrayList<String>(security.getIgnored());
		if (ignored.isEmpty()) {
			ignored.addAll(DEFAULT_IGNORED);
		}
		else if (ignored.contains("none")) {
			ignored.remove("none");
		}
		return ignored;
	}

	// Get the ignored paths in early
	@Order(SecurityProperties.IGNORED_ORDER)
	private static class IgnoredPathsWebSecurityConfigurerAdapter
			implements WebSecurityConfigurer<WebSecurity> {

		@Autowired(required = false)
		private ErrorController errorController;

		@Autowired
		private SecurityProperties security;

		@Autowired
		private ServerProperties server;

		@Override
		public void configure(WebSecurity builder) throws Exception {
		}

		@Override
		public void init(WebSecurity builder) throws Exception {
			List<String> ignored = getIgnored(this.security);
			if (this.errorController != null) {
				ignored.add(normalizePath(this.errorController.getErrorPath()));
			}
			String[] paths = this.server.getPathsArray(ignored);
			if (!ObjectUtils.isEmpty(paths)) {
				builder.ignoring().antMatchers(paths);
			}
		}

		private String normalizePath(String errorPath) {
			String result = StringUtils.cleanPath(errorPath);
			if (!result.startsWith("/")) {
				result = "/" + result;
			}
			return result;
		}

	}

	@Configuration
	@ConditionalOnProperty(prefix = "security.basic", name = "enabled", havingValue = "false")
	@Order(SecurityProperties.BASIC_AUTH_ORDER)
	protected static class ApplicationNoWebSecurityConfigurerAdapter
			extends WebSecurityConfigurerAdapter {
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.requestMatcher(new RequestMatcher() {
				@Override
				public boolean matches(HttpServletRequest request) {
					return false;
				}
			});
		}
	}

	@Configuration
	@ConditionalOnProperty(prefix = "security.basic", name = "enabled", matchIfMissing = true)
	@Order(SecurityProperties.BASIC_AUTH_ORDER)
	protected static class ApplicationWebSecurityConfigurerAdapter
			extends WebSecurityConfigurerAdapter {

		@Autowired
		private SecurityProperties security;

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			if (this.security.isRequireSsl()) {
				http.requiresChannel().anyRequest().requiresSecure();
			}
			if (!this.security.isEnableCsrf()) {
				http.csrf().disable();
			}
			// No cookies for application endpoints by default
			http.sessionManagement().sessionCreationPolicy(this.security.getSessions());
			SpringBootWebSecurityConfiguration.configureHeaders(http.headers(),
					this.security.getHeaders());
			String[] paths = getSecureApplicationPaths();
			if (paths.length > 0) {
				AuthenticationEntryPoint entryPoint = entryPoint();
				http.exceptionHandling().authenticationEntryPoint(entryPoint);
				http.httpBasic().authenticationEntryPoint(entryPoint);
				http.requestMatchers().antMatchers(paths);
				String[] roles = this.security.getUser().getRole().toArray(new String[0]);
				SecurityAuthorizeMode mode = this.security.getBasic().getAuthorizeMode();
				if (mode == null || mode == SecurityAuthorizeMode.ROLE) {
					http.authorizeRequests().anyRequest().hasAnyRole(roles);
				}
				else if (mode == SecurityAuthorizeMode.AUTHENTICATED) {
					http.authorizeRequests().anyRequest().authenticated();
				}
			}
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

	}

}
