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

package org.springframework.boot.autoconfigure.h2;

import org.h2.server.web.WebServlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityAuthorizeMode;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for H2's web console.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(WebServlet.class)
@ConditionalOnProperty(prefix = "spring.h2.console", name = "enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(H2ConsoleProperties.class)
@AutoConfigureAfter(SecurityAutoConfiguration.class)
public class H2ConsoleAutoConfiguration {

	@Autowired
	private H2ConsoleProperties properties;

	@Bean
	public ServletRegistrationBean h2Console() {
		String path = this.properties.getPath();
		String urlMapping = (path.endsWith("/") ? path + "*" : path + "/*");
		return new ServletRegistrationBean(new WebServlet(), urlMapping);
	}

	@Configuration
	@ConditionalOnClass(WebSecurityConfigurerAdapter.class)
	@ConditionalOnBean(ObjectPostProcessor.class)
	@ConditionalOnProperty(prefix = "security.basic", name = "enabled", matchIfMissing = true)
	static class H2ConsoleSecurityConfiguration {

		@Bean
		public WebSecurityConfigurerAdapter h2ConsoleSecurityConfigurer() {
			return new H2ConsoleSecurityConfigurer();
		}

		@Order(SecurityProperties.BASIC_AUTH_ORDER - 10)
		private static class H2ConsoleSecurityConfigurer
				extends WebSecurityConfigurerAdapter {

			@Autowired
			private H2ConsoleProperties console;

			@Autowired
			private SecurityProperties security;

			@Override
			public void configure(HttpSecurity http) throws Exception {
				String path = this.console.getPath();
				String antPattern = (path.endsWith("/") ? path + "**" : path + "/**");
				HttpSecurity h2Console = http.antMatcher(antPattern);
				h2Console.csrf().disable();
				h2Console.httpBasic();
				h2Console.headers().frameOptions().sameOrigin();
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

	}

}
