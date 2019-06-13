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

package org.springframework.boot.autoconfigure.security.oauth2.resource.reactive;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.introspection.NimbusReactiveOAuth2TokenIntrospectionClient;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOAuth2TokenIntrospectionClient;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configures a {@link ReactiveOAuth2TokenIntrospectionClient} when a token introspection
 * endpoint is available. Also configures a {@link SecurityWebFilterChain} if a
 * {@link ReactiveOAuth2TokenIntrospectionClient} bean is found.
 *
 * @author Madhura Bhave
 */
class ReactiveOAuth2ResourceServerOpaqueTokenConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ReactiveOAuth2TokenIntrospectionClient.class)
	static class OpaqueTokenIntrospectionClientConfiguration {

		@Bean
		@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.opaque-token.introspection-uri")
		public NimbusReactiveOAuth2TokenIntrospectionClient oAuth2TokenIntrospectionClient(
				OAuth2ResourceServerProperties properties) {
			OAuth2ResourceServerProperties.OpaqueToken opaqueToken = properties.getOpaqueToken();
			return new NimbusReactiveOAuth2TokenIntrospectionClient(opaqueToken.getIntrospectionUri(),
					opaqueToken.getClientId(), opaqueToken.getClientSecret());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(SecurityWebFilterChain.class)
	static class WebSecurityConfiguration {

		@Bean
		@ConditionalOnBean(ReactiveOAuth2TokenIntrospectionClient.class)
		public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
			http.authorizeExchange().anyExchange().authenticated().and().oauth2ResourceServer().opaqueToken();
			return http.build();
		}

	}

}
