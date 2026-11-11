/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet;

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

	OAuth2AuthorizationServerConfiguration(OAuth2AuthorizationServerProperties properties) {
		this.propertiesMapper = new OAuth2AuthorizationServerPropertiesMapper(properties);
	}

	@Bean
	@ConditionalOnMissingBean
	@Conditional(RegisteredClientsConfiguredCondition.class)
	RegisteredClientRepository registeredClientRepository() {
		return new InMemoryRegisteredClientRepository(this.propertiesMapper.asRegisteredClients());
	}

	@Bean
	@ConditionalOnMissingBean
	AuthorizationServerSettings authorizationServerSettings() {
		return this.propertiesMapper.asAuthorizationServerSettings();
	}

}
