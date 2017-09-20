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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

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
@ConditionalOnClass({ EnableWebSecurity.class, ClientRegistration.class })
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class ClientRegistrationRepositoryAutoConfiguration {

	@Configuration
	@Conditional(ClientsConfiguredCondition.class)
	@ConditionalOnMissingBean(ClientRegistrationRepository.class)
	protected static class ClientRegistrationRepositoryConfiguration {

		@Bean
		public ClientRegistrationRepository clientRegistrationRepository(
				Environment environment, OAuth2ClientProperties oauth2ClientProperties) {

			this.mergeClientDefaults(environment, oauth2ClientProperties);

			List<ClientRegistration> clientRegistrations = new ArrayList<>();


			oauth2ClientProperties.getRegistrations().values().stream()
					.filter(e -> !ObjectUtils.isEmpty(e.getClientId()))
					.collect(Collectors.toList())
					.forEach(clientProperties -> clientRegistrations.add(
							new ClientRegistration.Builder(clientProperties).build()));

			return new InMemoryClientRegistrationRepository(clientRegistrations);
		}

		private void mergeClientDefaults(Environment environment, OAuth2ClientProperties oauth2ClientProperties) {
			Assert.isInstanceOf(ConfigurableEnvironment.class, environment, "environment must be a ConfigurableEnvironment");

			MutablePropertySources propertySources = new MutablePropertySources();
			((ConfigurableEnvironment) environment).getPropertySources().forEach(propertySource -> {
				if (MapPropertySource.class.isAssignableFrom(propertySource.getClass())) {
					propertySources.addLast(propertySource);
				}
			});
			propertySources.addLast(ClientPropertiesUtil.loadDefaultsPropertySource());

			Map<String, ClientRegistrationProperties> mergedClients =
					new Binder(ConfigurationPropertySources.from(propertySources))
							.bind(ClientPropertiesUtil.CLIENT_REGISTRATIONS_PROPERTY_PREFIX,
									Bindable.mapOf(String.class, ClientRegistrationProperties.class))
							.get();

			// Override with merged client properties
			oauth2ClientProperties.setRegistrations(mergedClients);
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
			Set<String> clientKeys = this.getClientKeys(context.getEnvironment());
			if (!clientKeys.isEmpty()) {
				return ConditionOutcome.match(message.foundExactly("OAuth2 Client(s) -> "
						+ clientKeys.stream().collect(Collectors.joining(", "))));
			}
			return ConditionOutcome.noMatch(message.notAvailable("OAuth2 Client(s)"));
		}

		private Set<String> getClientKeys(Environment environment) {
			Map<String, ClientRegistrationProperties> clientsProperties = Binder.get(environment)
					.bind(ClientPropertiesUtil.CLIENT_REGISTRATIONS_PROPERTY_PREFIX,
							Bindable.mapOf(String.class, ClientRegistrationProperties.class))
					.orElse(new HashMap<>());

			// Filter out clients that don't have the client-id property set
			return clientsProperties.entrySet().stream()
					.filter(e -> !ObjectUtils.isEmpty(e.getValue().getClientId()))
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
		}
	}
}
