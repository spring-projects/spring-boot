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

package org.springframework.boot.autoconfigure.security.oauth2.server.servlet;

import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
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

	/**
     * Configures the security filter chain for the authorization server.
     * This filter chain is responsible for handling security aspects of the authorization server.
     * The filter chain is applied with the highest precedence.
     *
     * @param http the HttpSecurity object to configure the security filter chain
     * @return the configured SecurityFilterChain for the authorization server
     * @throws Exception if an error occurs during configuration
     */
    @Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		http.getConfigurer(OAuth2AuthorizationServerConfigurer.class).oidc(withDefaults());
		http.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
		http.exceptionHandling((exceptions) -> exceptions.defaultAuthenticationEntryPointFor(
				new LoginUrlAuthenticationEntryPoint("/login"), createRequestMatcher()));
		return http.build();
	}

	/**
     * Configures the default security filter chain for the OAuth2 authorization server.
     * This filter chain is responsible for handling authentication and authorization for incoming HTTP requests.
     * The filter chain is ordered with the BASIC_AUTH_ORDER to ensure that basic authentication is applied before other authentication mechanisms.
     * 
     * @param http the HttpSecurity object used to configure the security filter chain
     * @return the configured SecurityFilterChain object
     * @throws Exception if an error occurs while configuring the security filter chain
     */
    @Bean
	@Order(SecurityProperties.BASIC_AUTH_ORDER)
	SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated()).formLogin(withDefaults());
		return http.build();
	}

	/**
     * Creates a request matcher for media types.
     * 
     * @return The created request matcher.
     */
    private static RequestMatcher createRequestMatcher() {
		MediaTypeRequestMatcher requestMatcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
		requestMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
		return requestMatcher;
	}

}
