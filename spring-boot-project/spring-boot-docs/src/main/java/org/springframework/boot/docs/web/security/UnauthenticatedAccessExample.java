/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docs.web.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * Example configuration for using a {@link WebSecurityConfigurerAdapter} to configure
 * unauthenticated access to the home page at "/".
 *
 * @author Robert Stern
 */
public class UnauthenticatedAccessExample {

	/**
	 * {@link WebSecurityConfigurerAdapter} that provides init to configure
	 * {@link WebSecurity} argument to customize access rules.
	 */
	// tag::configuration[]
	@Configuration(proxyBeanMethods = false)
	static class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

		@Override
		public void init(WebSecurity web) {
			web.ignoring().antMatchers("/");
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.antMatcher("/**").authorizeRequests().anyRequest().authenticated();
		}

	}
	// end::configuration[]

}
