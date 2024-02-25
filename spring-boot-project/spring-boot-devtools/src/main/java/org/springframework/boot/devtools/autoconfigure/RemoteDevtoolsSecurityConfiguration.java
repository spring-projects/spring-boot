/*
 * Copyright 2012-2023 the original author or authors.
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration that allows anonymous access to the remote devtools
 * endpoint.
 *
 * @author Madhura Bhave
 */
@ConditionalOnClass({ SecurityFilterChain.class, HttpSecurity.class })
@Configuration(proxyBeanMethods = false)
class RemoteDevtoolsSecurityConfiguration {

	private final String url;

	/**
     * Constructs a new RemoteDevtoolsSecurityConfiguration object with the specified DevToolsProperties and ServerProperties.
     * 
     * @param devToolsProperties the DevToolsProperties object containing the configuration for DevTools
     * @param serverProperties the ServerProperties object containing the configuration for the server
     */
    RemoteDevtoolsSecurityConfiguration(DevToolsProperties devToolsProperties, ServerProperties serverProperties) {
		ServerProperties.Servlet servlet = serverProperties.getServlet();
		String servletContextPath = (servlet.getContextPath() != null) ? servlet.getContextPath() : "";
		this.url = servletContextPath + devToolsProperties.getRemote().getContextPath() + "/restart";
	}

	/**
     * Creates a security filter chain for the devtools endpoint.
     * This filter chain allows anonymous access to all requests.
     * CSRF protection is disabled for this endpoint.
     *
     * @param http the HttpSecurity object to configure the security filter chain
     * @return the configured security filter chain
     * @throws Exception if an error occurs while configuring the security filter chain
     */
    @Bean
	@Order(SecurityProperties.BASIC_AUTH_ORDER - 1)
	SecurityFilterChain devtoolsSecurityFilterChain(HttpSecurity http) throws Exception {
		http.securityMatcher(new AntPathRequestMatcher(this.url));
		http.authorizeHttpRequests((requests) -> requests.anyRequest().anonymous());
		http.csrf((csrf) -> csrf.disable());
		return http.build();
	}

}
