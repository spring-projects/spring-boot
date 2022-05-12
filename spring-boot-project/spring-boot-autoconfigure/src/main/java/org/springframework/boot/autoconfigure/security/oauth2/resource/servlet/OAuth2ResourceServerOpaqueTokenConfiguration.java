/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource.servlet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures an {@link OpaqueTokenIntrospector} when a token introspection endpoint is
 * available. Also configures a {@link SecurityFilterChain} if a
 * {@link OpaqueTokenIntrospector} bean is found.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
class OAuth2ResourceServerOpaqueTokenConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(OpaqueTokenIntrospector.class)
	static class OpaqueTokenIntrospectionClientConfiguration {

		@Bean
		@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri")
		SpringOpaqueTokenIntrospector opaqueTokenIntrospector(OAuth2ResourceServerProperties properties) {
			OAuth2ResourceServerProperties.Opaquetoken opaqueToken = properties.getOpaquetoken();
			return new SpringOpaqueTokenIntrospector(opaqueToken.getIntrospectionUri(), opaqueToken.getClientId(),
					opaqueToken.getClientSecret());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnDefaultWebSecurity
	static class OAuth2SecurityFilterChainConfiguration {

		@Bean
		@ConditionalOnBean(OpaqueTokenIntrospector.class)
		SecurityFilterChain opaqueTokenSecurityFilterChain(HttpSecurity http) throws Exception {
			http.authorizeRequests((requests) -> requests.anyRequest().authenticated());
			http.oauth2ResourceServer(OAuth2ResourceServerConfigurer::opaqueToken);
			return http.build();
		}

	}

}
