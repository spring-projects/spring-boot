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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties.Headers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity.IgnoredRequestConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.header.writers.HstsHeaderWriter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.web.servlet.support.RequestDataValueProcessor;

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
@EnableConfigurationProperties
@ConditionalOnClass({ EnableWebSecurity.class })
@ConditionalOnMissingBean(WebSecurityConfiguration.class)
@ConditionalOnWebApplication
public class SpringBootWebSecurityConfiguration {

	private static List<String> DEFAULT_IGNORED = Arrays.asList("/css/**", "/js/**",
			"/images/**", "/**/favicon.ico");

	@Bean
	@ConditionalOnMissingBean
	public AuthenticationEventPublisher authenticationEventPublisher() {
		return new DefaultAuthenticationEventPublisher();
	}

	@Bean
	@ConditionalOnMissingBean({ IgnoredPathsWebSecurityConfigurerAdapter.class })
	public WebSecurityConfigurer<WebSecurity> ignoredPathsWebSecurityConfigurerAdapter() {
		return new IgnoredPathsWebSecurityConfigurerAdapter();
	}

	public static void configureHeaders(HeadersConfigurer<?> configurer,
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
	@Order(Ordered.HIGHEST_PRECEDENCE)
	private static class IgnoredPathsWebSecurityConfigurerAdapter implements
			WebSecurityConfigurer<WebSecurity> {

		@Autowired
		private SecurityProperties security;

		@Override
		public void configure(WebSecurity builder) throws Exception {
		}

		@Override
		public void init(WebSecurity builder) throws Exception {
			IgnoredRequestConfigurer ignoring = builder.ignoring();
			List<String> ignored = getIgnored(this.security);
			ignoring.antMatchers(ignored.toArray(new String[0]));
		}

	}

	// Pull in @EnableWebMvcSecurity if Spring MVC is available and no-one defined a
	// RequestDataValueProcessor
	@ConditionalOnClass(RequestDataValueProcessor.class)
	@ConditionalOnMissingBean(RequestDataValueProcessor.class)
	@ConditionalOnExpression("${security.basic.enabled:true}")
	@Configuration
	protected static class WebMvcSecurityConfigurationConditions {

		@Configuration
		@EnableWebMvcSecurity
		protected static class DefaultWebMvcSecurityConfiguration {

		}

	}

	// Pull in a plain @EnableWebSecurity if Spring MVC is not available
	@ConditionalOnMissingBean(WebMvcSecurityConfigurationConditions.class)
	@ConditionalOnMissingClass(name = "org.springframework.web.servlet.support.RequestDataValueProcessor")
	@ConditionalOnExpression("${security.basic.enabled:true}")
	@Configuration
	@EnableWebSecurity
	protected static class DefaultWebSecurityConfiguration {

	}

	@ConditionalOnExpression("${security.basic.enabled:true}")
	@Configuration
	@Order(Ordered.LOWEST_PRECEDENCE - 5)
	protected static class ApplicationWebSecurityConfigurerAdapter extends
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
				http.authorizeRequests()
						.anyRequest()
						.hasAnyRole(
								this.security.getUser().getRole().toArray(new String[0])) //
						.and().httpBasic() //
						.and().anonymous().disable();
			}
			if (!this.security.isEnableCsrf()) {
				http.csrf().disable();
			}
			// No cookies for application endpoints by default
			http.sessionManagement().sessionCreationPolicy(this.security.getSessions());

			SpringBootWebSecurityConfiguration.configureHeaders(http.headers(),
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

}
