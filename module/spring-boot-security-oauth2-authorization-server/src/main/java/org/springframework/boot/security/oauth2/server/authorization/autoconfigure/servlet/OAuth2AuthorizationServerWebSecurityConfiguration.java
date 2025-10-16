/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet;

import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * {@link Configuration @Configuration} for OAuth2 authorization server support.
 *
 * @author Steve Riesenberg
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnDefaultWebSecurity
@ConditionalOnBean({ RegisteredClientRepository.class, AuthorizationServerSettings.class })
class OAuth2AuthorizationServerWebSecurityConfiguration {

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) {
		OAuth2AuthorizationServerConfigurer authorizationServer = new OAuth2AuthorizationServerConfigurer();
		http.securityMatcher(authorizationServer.getEndpointsMatcher());
		http.with(authorizationServer, withDefaults());
		http.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated());
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class).oidc(withDefaults());
		http.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
		http.exceptionHandling((exceptions) -> exceptions.defaultAuthenticationEntryPointFor(
				new LoginUrlAuthenticationEntryPoint("/login"), createRequestMatcher()));
		return http.build();
	}

	@Bean
	@Order(SecurityFilterProperties.BASIC_AUTH_ORDER)
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
		http.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated()).formLogin(withDefaults());
		return http.build();
	}

	private static RequestMatcher createRequestMatcher() {
		MediaTypeRequestMatcher requestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
		requestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
		return requestMatcher;
	}

}
