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

package org.springframework.boot.autoconfigure.security.oauth2.client.servlet;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientConnectionDetails;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientConnectionDetailsMapper;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.PropertiesOAuth2ClientConnectionDetails;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * {@link Configuration @Configuration} used to map {@link OAuth2ClientConnectionDetails}
 * to client registrations.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OAuth2ClientProperties.class)
class OAuth2ClientRegistrationRepositoryConfiguration {

	@Bean
	@ConditionalOnMissingBean(OAuth2ClientConnectionDetails.class)
	@Conditional(ClientsConfiguredCondition.class)
	PropertiesOAuth2ClientConnectionDetails oAuth2ClientConnectionDetails(OAuth2ClientProperties properties) {
		return new PropertiesOAuth2ClientConnectionDetails(properties);
	}

	@Bean
	@ConditionalOnBean(OAuth2ClientConnectionDetails.class)
	@ConditionalOnMissingBean(ClientRegistrationRepository.class)
	InMemoryClientRegistrationRepository clientRegistrationRepository(OAuth2ClientConnectionDetails connectionDetails) {
		List<ClientRegistration> registrations = new ArrayList<>(
				new OAuth2ClientConnectionDetailsMapper(connectionDetails).asClientRegistrations().values());
		return new InMemoryClientRegistrationRepository(registrations);
	}

}
