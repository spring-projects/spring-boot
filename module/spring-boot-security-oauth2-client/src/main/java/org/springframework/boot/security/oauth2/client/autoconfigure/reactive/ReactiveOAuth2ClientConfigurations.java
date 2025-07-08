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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.ConditionalOnOAuth2ClientRegistrationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;

/**
 * Reactive OAuth2 Client configurations.
 *
 * @author Madhura Bhave
 */
class ReactiveOAuth2ClientConfigurations {

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(OAuth2ClientProperties.class)
	@ConditionalOnOAuth2ClientRegistrationProperties
	@ConditionalOnMissingBean(ReactiveClientRegistrationRepository.class)
	static class ReactiveClientRegistrationRepositoryConfiguration {

		@Bean
		InMemoryReactiveClientRegistrationRepository reactiveClientRegistrationRepository(
				OAuth2ClientProperties properties) {
			List<ClientRegistration> registrations = new ArrayList<>(
					new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().values());
			return new InMemoryReactiveClientRegistrationRepository(registrations);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(ReactiveClientRegistrationRepository.class)
	static class ReactiveOAuth2AuthorizedClientServiceConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ReactiveOAuth2AuthorizedClientService reactiveAuthorizedClientService(
				ReactiveClientRegistrationRepository clientRegistrationRepository) {
			return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
		}

	}

}
