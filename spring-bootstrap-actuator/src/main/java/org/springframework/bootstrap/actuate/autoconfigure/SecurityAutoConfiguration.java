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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.actuate.properties.EndpointsProperties;
import org.springframework.bootstrap.actuate.properties.SecurityProperties;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.context.annotation.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ EnableWebSecurity.class })
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean({ AuthenticationEventPublisher.class })
	public AuthenticationEventPublisher authenticationEventPublisher() {
		return new DefaultAuthenticationEventPublisher();
	}

	@Bean
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
				http.requiresChannel().antMatchers("/**").requiresSecure();
			}
			if (this.security.getBasic().isEnabled()) {
				http.authenticationEntryPoint(entryPoint())
						.antMatcher(this.security.getBasic().getPath()).httpBasic()
						.authenticationEntryPoint(entryPoint()).and().anonymous()
						.disable();
				http.authorizeUrls().antMatchers("/**")
						.hasRole(this.security.getBasic().getRole());
			}
			// No cookies for service endpoints by default
			http.sessionManagement().sessionCreationPolicy(this.security.getSessions());
		}

		private AuthenticationEntryPoint entryPoint() {
			BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
			entryPoint.setRealmName(this.security.getBasic().getRealm());
			return entryPoint;
		}

		@Override
		public void configure(WebSecurityBuilder builder) throws Exception {
			builder.ignoring().antMatchers(this.endpoints.getHealth().getPath(),
					this.endpoints.getInfo().getPath(),
					this.endpoints.getError().getPath());
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
