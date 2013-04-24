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

package org.springframework.bootstrap.autoconfigure.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.bootstrap.context.annotation.ConditionalOnClass;
import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean;
import org.springframework.bootstrap.service.annotation.EnableConfigurationProperties;
import org.springframework.bootstrap.service.properties.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.AuthenticationBuilder;
import org.springframework.security.config.annotation.web.EnableWebSecurity;
import org.springframework.security.config.annotation.web.ExpressionUrlAuthorizations;
import org.springframework.security.config.annotation.web.HttpConfiguration;
import org.springframework.security.config.annotation.web.SpringSecurityFilterChainBuilder.IgnoredRequestRegistry;
import org.springframework.security.config.annotation.web.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.WebSecurityConfigurerAdapter;
import org.springframework.stereotype.Component;

/**
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ EnableWebSecurity.class })
@ConditionalOnMissingBean({ WebSecurityConfiguration.class })
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {

	@Component
	public static class WebSecurityAdapter extends WebSecurityConfigurerAdapter {

		@Value("${endpoints.healthz.path:/healthz}")
		private String healthzPath = "/healthz";

		@Autowired
		private SecurityProperties security;

		@Override
		protected void ignoredRequests(IgnoredRequestRegistry ignoredRequests) {
			ignoredRequests.antMatchers(this.healthzPath);
		}

		@Override
		protected void authorizeUrls(ExpressionUrlAuthorizations interceptUrls) {
			interceptUrls.antMatchers("/**").hasRole("USER");
		}

		@Override
		protected void configure(HttpConfiguration http) throws Exception {
			http.antMatcher("/**").httpBasic().and().anonymous().disable();
			if (this.security.isRequireSsl()) {
				http.requiresChannel().antMatchers("/**").requiresSecure();
			}
		}

	}

	@ConditionalOnMissingBean(AuthenticationManager.class)
	@Configuration
	public static class AuthenticationManagerConfiguration {

		@Bean
		public AuthenticationManager authenticationManager() throws Exception {
			return new AuthenticationBuilder().inMemoryAuthentication().withUser("user")
					.password("password").roles("USER").and().and().build();
		}

	}
}
