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

import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security OAuth 2.0 / OpenID Connect 1.0 client support.
 *
 * @author Joe Grandja
 * @since 2.0.0
 */
@Configuration
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({EnableWebSecurity.class, ClientRegistration.class})
@AutoConfigureBefore(SecurityAutoConfiguration.class)
public class OAuth2ClientAutoConfiguration {

	private static final String CLIENT_PROPERTY_PREFIX = "security.oauth2.client";

	private static final String CLIENT_ID_PROPERTY = "client-id";

	private static final String USER_INFO_URI_PROPERTY = "user-info-uri";

	private static final String USER_NAME_ATTR_NAME_PROPERTY = "user-name-attribute-name";

	static Set<String> getClientKeys(Environment environment) {
		return getClientPropertiesByClient(environment).keySet();
	}

	static Map<String, Map> getClientPropertiesByClient(Environment environment) {
		Map<String, Object> clientPropertiesByClient = Binder.get(environment)
				.bind(CLIENT_PROPERTY_PREFIX, Bindable.mapOf(String.class, Object.class))
				.orElse(new HashMap<>());

		// Filter out clients that don't have the client-id property set
		return clientPropertiesByClient.entrySet().stream()
				.map(e -> new AbstractMap.SimpleImmutableEntry<>(e.getKey(), (Map) e.getValue()))
				.filter(e -> e.getValue().containsKey(CLIENT_ID_PROPERTY))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Order(1)
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
			getClientKeys(this.environment).forEach(clientKey -> {
				String fullClientKey = CLIENT_PROPERTY_PREFIX + "." + clientKey;
				ClientRegistrationProperties clientRegistrationProperties = binder.bind(
						fullClientKey, Bindable.of(ClientRegistrationProperties.class)).get();
				clientRegistrations.add(new ClientRegistration.Builder(clientRegistrationProperties).build());
			});

			return new InMemoryClientRegistrationRepository(clientRegistrations);
		}
	}

	@Order(2)
	@ConditionalOnMissingBean(WebSecurityConfiguration.class)
	@ConditionalOnBean(ClientRegistrationRepository.class)
	@EnableWebSecurity
	protected static class OAuth2LoginConfiguration extends WebSecurityConfigurerAdapter {
		private final Environment environment;
		private final ClientRegistrationRepository clientRegistrationRepository;

		protected OAuth2LoginConfiguration(Environment environment, ClientRegistrationRepository clientRegistrationRepository) {
			this.environment = environment;
			this.clientRegistrationRepository = clientRegistrationRepository;
		}

		// @formatter:off
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
				.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.oauth2Login()
					.clients(this.clientRegistrationRepository);

			this.registerUserNameAttributeNames(http.oauth2Login());
		}
		// @formatter:on

		private void registerUserNameAttributeNames(OAuth2LoginConfigurer<HttpSecurity> oauth2LoginConfigurer) throws Exception {
			getClientPropertiesByClient(this.environment).entrySet().stream().forEach(e -> {
				String userInfoUriValue = (String) e.getValue().get(USER_INFO_URI_PROPERTY);
				String userNameAttributeNameValue = (String) e.getValue().get(USER_NAME_ATTR_NAME_PROPERTY);
				if (userInfoUriValue != null && userNameAttributeNameValue != null) {
					// @formatter:off
					oauth2LoginConfigurer
						.userInfoEndpoint()
							.userNameAttributeName(userNameAttributeNameValue, URI.create(userInfoUriValue));
					// @formatter:on
				}
			});
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
			Set<String> clientKeys = getClientKeys(context.getEnvironment());
			if (!clientKeys.isEmpty()) {
				return ConditionOutcome.match(message.foundExactly("OAuth2 Client(s) -> " +
						clientKeys.stream().collect(Collectors.joining(", "))));
			}
			return ConditionOutcome.noMatch(message.notAvailable("OAuth2 Client(s)"));
		}
	}
}
