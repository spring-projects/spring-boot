/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Registration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * {@link Configuration} used to map {@link OAuth2ClientProperties} to client
 * registrations.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
@Configuration
@EnableConfigurationProperties(OAuth2ClientProperties.class)
@Conditional(OAuth2ClientRegistrationRepositoryConfiguration.ClientsConfiguredCondition.class)
class OAuth2ClientRegistrationRepositoryConfiguration {

	private final OAuth2ClientProperties properties;

	OAuth2ClientRegistrationRepositoryConfiguration(OAuth2ClientProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(ClientRegistrationRepository.class)
	public InMemoryClientRegistrationRepository clientRegistrationRepository() {
		List<ClientRegistration> registrations = new ArrayList<>(
				OAuth2ClientPropertiesRegistrationAdapter
						.getClientRegistrations(this.properties).values());
		return new InMemoryClientRegistrationRepository(registrations);
	}

	/**
	 * Condition that matches if any {@code spring.security.oauth2.client.registration}
	 * properties are defined.
	 */
	static class ClientsConfiguredCondition extends SpringBootCondition {

		private static final Bindable<Map<String, Registration>> BINDABLE_REGISTRATION = Bindable
				.mapOf(String.class, OAuth2ClientProperties.Registration.class);

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("OAuth2 Clients Configured Condition");
			Map<String, Registration> registrations = this
					.getRegistrations(context.getEnvironment());
			if (!registrations.isEmpty()) {
				return ConditionOutcome.match(message.foundExactly(
						"registered clients " + registrations.values().stream()
								.map(OAuth2ClientProperties.Registration::getClientId)
								.collect(Collectors.joining(", "))));
			}
			return ConditionOutcome.noMatch(message.notAvailable("registered clients"));
		}

		private Map<String, Registration> getRegistrations(Environment environment) {
			return Binder.get(environment)
					.bind("spring.security.oauth2.client.registration",
							BINDABLE_REGISTRATION)
					.orElse(Collections.emptyMap());
		}

	}

}
