/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security's Reactive
 * OAuth2 client.
 *
 * @author Madhura Bhave
 * @since 2.1.0
 */
@Configuration
@AutoConfigureBefore(ReactiveSecurityAutoConfiguration.class)
@EnableConfigurationProperties(OAuth2ClientProperties.class)
@Conditional(ReactiveOAuth2ClientAutoConfiguration.NonServletApplicationCondition.class)
@ConditionalOnClass({ Flux.class, EnableWebFluxSecurity.class, ClientRegistration.class })
public class ReactiveOAuth2ClientAutoConfiguration {

	private final OAuth2ClientProperties properties;

	public ReactiveOAuth2ClientAutoConfiguration(OAuth2ClientProperties properties) {
		this.properties = properties;
	}

	@Bean
	@Conditional(ClientsConfiguredCondition.class)
	@ConditionalOnMissingBean(ReactiveClientRegistrationRepository.class)
	public InMemoryReactiveClientRegistrationRepository clientRegistrationRepository() {
		List<ClientRegistration> registrations = new ArrayList<>(
				OAuth2ClientPropertiesRegistrationAdapter
						.getClientRegistrations(this.properties).values());
		return new InMemoryReactiveClientRegistrationRepository(registrations);
	}

	@Bean
	@ConditionalOnBean(ReactiveClientRegistrationRepository.class)
	@ConditionalOnMissingBean
	public ReactiveOAuth2AuthorizedClientService authorizedClientService(
			ReactiveClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryReactiveOAuth2AuthorizedClientService(
				clientRegistrationRepository);
	}

	@Bean
	@ConditionalOnBean(ReactiveOAuth2AuthorizedClientService.class)
	@ConditionalOnMissingBean
	public ServerOAuth2AuthorizedClientRepository authorizedClientRepository(
			ReactiveOAuth2AuthorizedClientService authorizedClientService) {
		return new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(
				authorizedClientService);
	}

	static class NonServletApplicationCondition extends NoneNestedConditions {

		NonServletApplicationCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
		static class ServletApplicationCondition {

		}

	}

}
