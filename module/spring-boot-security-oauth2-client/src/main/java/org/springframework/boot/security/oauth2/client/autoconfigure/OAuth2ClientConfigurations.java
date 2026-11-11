/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * Configurations related to auto-configuration of OAuth2 client support.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class OAuth2ClientConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnOAuth2ClientRegistrationProperties
	@EnableConfigurationProperties(OAuth2ClientProperties.class)
	static class ClientRegistrationRepositoryConfiguration {

		@Bean
		@ConditionalOnMissingBean(ClientRegistrationRepository.class)
		InMemoryClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
			List<ClientRegistration> registrations = new ArrayList<>(
					new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().values());
			return new InMemoryClientRegistrationRepository(registrations);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ClientRegistrationRepository.class)
	static class OAuth2AuthorizedClientServiceConfiguration {

		@Bean
		@ConditionalOnMissingBean
		OAuth2AuthorizedClientService authorizedClientService(
				ClientRegistrationRepository clientRegistrationRepository) {
			return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
		}

	}

}
