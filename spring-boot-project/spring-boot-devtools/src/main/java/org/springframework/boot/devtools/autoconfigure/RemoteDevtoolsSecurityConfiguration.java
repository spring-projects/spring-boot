/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.devtools.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration that allows anonymous access to the remote devtools
 * endpoint.
 *
 * @author Madhura Bhave
 */
@ConditionalOnClass({ SecurityFilterChain.class, WebSecurityConfigurerAdapter.class })
@Configuration(proxyBeanMethods = false)
class RemoteDevtoolsSecurityConfiguration {

	@Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
	@Configuration
	static class SecurityConfiguration extends WebSecurityConfigurerAdapter {

		private final String url;

		SecurityConfiguration(DevToolsProperties devToolsProperties, ServerProperties serverProperties) {
			ServerProperties.Servlet servlet = serverProperties.getServlet();
			String servletContextPath = (servlet.getContextPath() != null) ? servlet.getContextPath() : "";
			this.url = servletContextPath + devToolsProperties.getRemote().getContextPath() + "/restart";
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.requestMatcher(new AntPathRequestMatcher(this.url)).authorizeRequests().anyRequest().anonymous().and()
					.csrf().disable();
		}

	}

}
