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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configures a {@link SecurityWebFilterChain} for Reactive OAuth2 resource server support
 * if a {@link ReactiveJwtDecoder} bean is present.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ReactiveJwtDecoder.class)
class ReactiveOAuth2ResourceServerWebSecurityConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http,
			ReactiveJwtDecoder jwtDecoder) {
		http.authorizeExchange().anyExchange().authenticated().and()
				.oauth2ResourceServer().jwt().jwtDecoder(jwtDecoder);
		return http.build();
	}

}
