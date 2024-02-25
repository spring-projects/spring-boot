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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

/**
 * {@link Configuration @Configuration} used to map
 * {@link OAuth2AuthorizationServerProperties} to registered clients and settings.
 *
 * @author Steve Riesenberg
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OAuth2AuthorizationServerProperties.class)
class OAuth2AuthorizationServerConfiguration {

	private final OAuth2AuthorizationServerPropertiesMapper propertiesMapper;

	/**
     * Constructs a new instance of OAuth2AuthorizationServerConfiguration with the specified properties.
     * 
     * @param properties the properties to be used for configuring the authorization server
     */
    OAuth2AuthorizationServerConfiguration(OAuth2AuthorizationServerProperties properties) {
		this.propertiesMapper = new OAuth2AuthorizationServerPropertiesMapper(properties);
	}

	/**
     * Creates a new instance of {@link RegisteredClientRepository} if no other bean of the same type is present in the application context.
     * This method is conditionally executed only if the {@link RegisteredClientsConfiguredCondition} condition is met.
     * 
     * @return a new instance of {@link InMemoryRegisteredClientRepository} initialized with the registered clients obtained from the properties mapper.
     */
    @Bean
	@ConditionalOnMissingBean
	@Conditional(RegisteredClientsConfiguredCondition.class)
	RegisteredClientRepository registeredClientRepository() {
		return new InMemoryRegisteredClientRepository(this.propertiesMapper.asRegisteredClients());
	}

	/**
     * Returns the AuthorizationServerSettings based on the properties mapper.
     * This method is annotated with @Bean and @ConditionalOnMissingBean to ensure that
     * if there is already a bean of type AuthorizationServerSettings present in the application context,
     * this method will not be executed.
     * 
     * @return the AuthorizationServerSettings object created based on the properties mapper
     */
    @Bean
	@ConditionalOnMissingBean
	AuthorizationServerSettings authorizationServerSettings() {
		return this.propertiesMapper.asAuthorizationServerSettings();
	}

}
