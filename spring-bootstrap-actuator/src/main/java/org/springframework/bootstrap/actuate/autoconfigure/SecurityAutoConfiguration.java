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

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.actuate.properties.EndpointsProperties;
import org.springframework.bootstrap.actuate.properties.SecurityProperties;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.EnableWebSecurity;
import org.springframework.security.config.annotation.web.HttpConfiguration;
import org.springframework.security.config.annotation.web.WebSecurityBuilder;
import org.springframework.security.config.annotation.web.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
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

				HttpConfiguration matcher = http.requestMatchers().antMatchers(paths);
				matcher.authenticationEntryPoint(entryPoint()).httpBasic()
						.authenticationEntryPoint(entryPoint()).and().anonymous()
						.disable();
				matcher.authorizeUrls().anyRequest()
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

	@Conditional(NoUserSuppliedAuthenticationManager.class)
	@Configuration
	public static class AuthenticationManagerConfiguration {

		@Bean
		public AuthenticationManager authenticationManager() throws Exception {
			return new AuthenticationManagerBuilder().inMemoryAuthentication()
					.withUser("user").password("password").roles("USER").and().and()
					.build();
		}

	}

	private static class NoUserSuppliedAuthenticationManager implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			String[] beans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
					context.getBeanFactory(), AuthenticationManager.class, false, false);
			for (String bean : beans) {
				if (!BeanIds.AUTHENTICATION_MANAGER.equals(bean)) {
					// Not the one supplied by Spring Security automatically
					return false;
				}
			}
			return true;
		}

	}

}
