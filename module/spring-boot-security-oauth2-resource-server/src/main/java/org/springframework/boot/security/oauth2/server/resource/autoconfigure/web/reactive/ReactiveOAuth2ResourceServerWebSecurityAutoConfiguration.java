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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.reactive;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.OAuth2ResourceServerSpec.JwtSpec;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OAuth2 resource server reactive
 * based web security.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 4.1.0
 */
@AutoConfiguration(
		before = { ReactiveManagementWebSecurityAutoConfiguration.class, ReactiveWebSecurityAutoConfiguration.class,
				ReactiveUserDetailsServiceAutoConfiguration.class },
		after = ReactiveOAuth2ResourceServerAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
@ConditionalOnDefaultWebSecurity
@ConditionalOnClass({ EnableWebFluxSecurity.class })
public final class ReactiveOAuth2ResourceServerWebSecurityAutoConfiguration {

	@Bean
	@ConditionalOnBean(ReactiveJwtDecoder.class)
	SecurityWebFilterChain reactiveJwtSecurityFilterChain(ServerHttpSecurity http,
			ReactiveJwtDecoder reactiveJwtDecoder) {
		Customizer<JwtSpec> jwtCustomizer = (jwt) -> jwt.jwtDecoder(reactiveJwtDecoder);
		http.authorizeExchange((exchanges) -> exchanges.anyExchange().authenticated());
		http.oauth2ResourceServer((server) -> server.jwt(jwtCustomizer));
		return http.build();
	}

	@Bean
	@ConditionalOnBean(ReactiveOpaqueTokenIntrospector.class)
	SecurityWebFilterChain reactiveOpaqueTokenSecurityWebFilterChain(ServerHttpSecurity http) {
		http.authorizeExchange((exchanges) -> exchanges.anyExchange().authenticated());
		http.oauth2ResourceServer((resourceServer) -> resourceServer.opaqueToken(withDefaults()));
		return http.build();
	}

}
