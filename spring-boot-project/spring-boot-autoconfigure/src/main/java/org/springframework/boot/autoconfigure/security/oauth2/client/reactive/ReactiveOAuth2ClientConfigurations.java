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

package org.springframework.boot.autoconfigure.security.oauth2.client.reactive;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Reactive OAuth2 Client configurations.
 *
 * @author Madhura Bhave
 */
class ReactiveOAuth2ClientConfigurations {

	/**
     * ReactiveClientRegistrationRepositoryConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@Conditional(ClientsConfiguredCondition.class)
	@ConditionalOnMissingBean(ReactiveClientRegistrationRepository.class)
	static class ReactiveClientRegistrationRepositoryConfiguration {

		/**
         * Creates an in-memory reactive client registration repository based on the provided OAuth2 client properties.
         *
         * @param properties the OAuth2 client properties used to configure the client registrations
         * @return the in-memory reactive client registration repository
         */
        @Bean
		InMemoryReactiveClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
			List<ClientRegistration> registrations = new ArrayList<>(
					new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().values());
			return new InMemoryReactiveClientRegistrationRepository(registrations);
		}

	}

	/**
     * ReactiveOAuth2ClientConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ReactiveClientRegistrationRepository.class)
	static class ReactiveOAuth2ClientConfiguration {

		/**
         * Creates a new instance of {@link ReactiveOAuth2AuthorizedClientService} if no other bean of the same type is present.
         * 
         * @param clientRegistrationRepository the {@link ReactiveClientRegistrationRepository} used to retrieve client registrations
         * @return the {@link ReactiveOAuth2AuthorizedClientService} instance
         */
        @Bean
		@ConditionalOnMissingBean
		ReactiveOAuth2AuthorizedClientService authorizedClientService(
				ReactiveClientRegistrationRepository clientRegistrationRepository) {
			return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
		}

		/**
         * Creates a new instance of {@link ServerOAuth2AuthorizedClientRepository} if no other bean of the same type is present.
         * This repository is responsible for managing the storage and retrieval of authorized OAuth2 clients for the server-side application.
         * It uses the provided {@link ReactiveOAuth2AuthorizedClientService} to interact with the underlying storage.
         *
         * @param authorizedClientService the service used for interacting with the underlying storage
         * @return the created instance of {@link ServerOAuth2AuthorizedClientRepository}
         */
        @Bean
		@ConditionalOnMissingBean
		ServerOAuth2AuthorizedClientRepository authorizedClientRepository(
				ReactiveOAuth2AuthorizedClientService authorizedClientService) {
			return new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(authorizedClientService);
		}

		/**
         * SecurityWebFilterChainConfiguration class.
         */
        @Configuration(proxyBeanMethods = false)
		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
		static class SecurityWebFilterChainConfiguration {

			/**
             * Configures the Spring Security filter chain for handling security in a web application.
             * This method is annotated with @Bean to indicate that it should be treated as a Spring bean and managed by the Spring container.
             * It is also annotated with @ConditionalOnMissingBean to ensure that this configuration is only applied if there is no existing bean of the same type.
             * 
             * @param http the ServerHttpSecurity object used to configure the security filter chain
             * @return the configured SecurityWebFilterChain object
             */
            @Bean
			@ConditionalOnMissingBean
			SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
				http.authorizeExchange((exchange) -> exchange.anyExchange().authenticated());
				http.oauth2Login(withDefaults());
				http.oauth2Client(withDefaults());
				return http.build();
			}

		}

	}

}
