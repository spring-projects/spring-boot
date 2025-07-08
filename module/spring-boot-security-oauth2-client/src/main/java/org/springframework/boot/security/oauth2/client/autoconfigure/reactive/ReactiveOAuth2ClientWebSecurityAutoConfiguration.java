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

package org.springframework.boot.security.oauth2.client.autoconfigure.reactive;

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.security.autoconfigure.actuate.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Auto-configuration for reactive web security that uses an OAuth 2 client.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(
		before = { ReactiveManagementWebSecurityAutoConfiguration.class, ReactiveSecurityAutoConfiguration.class },
		after = ReactiveOAuth2ClientAutoConfiguration.class)
@ConditionalOnClass({ Flux.class, EnableWebFluxSecurity.class, ServerOAuth2AuthorizedClientRepository.class })
@ConditionalOnBean(ReactiveOAuth2AuthorizedClientService.class)
@ConditionalOnWebApplication(type = Type.REACTIVE)
public class ReactiveOAuth2ClientWebSecurityAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ServerOAuth2AuthorizedClientRepository authorizedClientRepository(
			ReactiveOAuth2AuthorizedClientService authorizedClientService) {
		return new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(authorizedClientService);
	}

	@Bean
	@ConditionalOnMissingBean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http.authorizeExchange((exchange) -> exchange.anyExchange().authenticated());
		http.oauth2Login(withDefaults());
		http.oauth2Client(withDefaults());
		return http.build();
	}

}
