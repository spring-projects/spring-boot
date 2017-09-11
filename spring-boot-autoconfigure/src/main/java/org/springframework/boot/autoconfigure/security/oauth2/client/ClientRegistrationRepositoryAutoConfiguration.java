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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a <code>ClientRegistrationRepository</code> bean,
 * which serves as a store for OAuth 2.0 / OpenID Connect 1.0 client registrations.
 *
 * @author Joe Grandja
 * @since 2.0.0
 * @see ClientRegistrationRepository
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({EnableWebSecurity.class, ClientRegistration.class})
public class ClientRegistrationRepositoryAutoConfiguration {

	@Configuration
	@Conditional(ClientsConfiguredCondition.class)
	@ConditionalOnMissingBean(ClientRegistrationRepository.class)
	protected static class ClientRegistrationRepositoryConfiguration {
		private final Environment environment;

		protected ClientRegistrationRepositoryConfiguration(Environment environment) {
			this.environment = environment;
		}

		@Bean
		public ClientRegistrationRepository clientRegistrationRepository() {
			List<ClientRegistration> clientRegistrations = new ArrayList<>();

			Binder binder = Binder.get(this.environment);
			ClientPropertiesUtil.getClientKeys(this.environment).forEach(clientKey -> {
				String fullClientKey = ClientPropertiesUtil.CLIENT_PROPERTY_PREFIX + "." + clientKey;
				ClientRegistrationProperties clientRegistrationProperties = binder.bind(
						fullClientKey, Bindable.of(ClientRegistrationProperties.class)).get();
				clientRegistrations.add(new ClientRegistration.Builder(clientRegistrationProperties).build());
			});

			return new InMemoryClientRegistrationRepository(clientRegistrations);
		}
	}

	private static class ClientsConfiguredCondition extends SpringBootCondition implements ConfigurationCondition {

		@Override
		public ConfigurationCondition.ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.PARSE_CONFIGURATION;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("OAuth2 Clients Configured Condition");
			Set<String> clientKeys = ClientPropertiesUtil.getClientKeys(context.getEnvironment());
			if (!clientKeys.isEmpty()) {
				return ConditionOutcome.match(message.foundExactly("OAuth2 Client(s) -> " +
						clientKeys.stream().collect(Collectors.joining(", "))));
			}
			return ConditionOutcome.noMatch(message.notAvailable("OAuth2 Client(s)"));
		}
	}

}
