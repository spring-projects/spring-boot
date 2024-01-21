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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientConnectionDetails;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Registration;
import org.springframework.boot.autoconfigure.security.oauth2.client.PropertiesOAuth2ClientConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2ClientRegistrationRepositoryConfiguration}.
 *
 * @author Madhura Bhave
 */
class OAuth2ClientRegistrationRepositoryConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration";

	@Test
	void clientRegistrationRepositoryBeanShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner.withUserConfiguration(OAuth2ClientRegistrationRepositoryConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class));
	}

	@Test
	void clientRegistrationRepositoryBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner.withUserConfiguration(OAuth2ClientRegistrationRepositoryConfiguration.class)
			.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
					REGISTRATION_PREFIX + ".foo.client-secret=secret", REGISTRATION_PREFIX + ".foo.provider=github")
			.run((context) -> {
				ClientRegistrationRepository repository = context.getBean(ClientRegistrationRepository.class);
				ClientRegistration registration = repository.findByRegistrationId("foo");
				assertThat(registration).isNotNull();
				assertThat(registration.getClientSecret()).isEqualTo("secret");
			});
	}

	@Test
	void clientRegistrationRepositoryBeanShouldBeCreatedWhenConnectionDetailsPresent() {
		this.contextRunner
			.withUserConfiguration(ConnectionDetailsClientConfiguration.class,
					OAuth2ClientRegistrationRepositoryConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(OAuth2ClientConnectionDetails.class)
					.doesNotHaveBean(PropertiesOAuth2ClientConnectionDetails.class);
				ClientRegistrationRepository repo = context.getBean(ClientRegistrationRepository.class);
				ClientRegistration registration = repo.findByRegistrationId("oauth2-client");
				assertThat(registration).isNotNull();
				assertThat(registration.getClientName()).isEqualTo("client");
				assertThat(registration.getClientId()).isEqualTo("client-id");
				assertThat(registration.getClientSecret()).isEqualTo("client-secret");
				assertThat(registration.getAuthorizationGrantType())
					.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
				assertThat(registration.getScopes()).contains("openid", "some-scope");
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsClientConfiguration {

		@Bean
		OAuth2ClientConnectionDetails oAuth2ClientConnectionDetails() {
			return new OAuth2ClientConnectionDetails() {
				@Override
				public Map<String, Registration> getRegistrations() {
					Registration registration = new Registration() {
						@Override
						public String getProvider() {
							return "github";
						}

						@Override
						public String getClientId() {
							return "client-id";
						}

						@Override
						public String getClientSecret() {
							return "client-secret";
						}

						@Override
						public String getClientAuthenticationMethod() {
							return null;
						}

						@Override
						public String getAuthorizationGrantType() {
							return AuthorizationGrantType.AUTHORIZATION_CODE.getValue();
						}

						@Override
						public String getRedirectUri() {
							return null;
						}

						@Override
						public Set<String> getScopes() {
							return Set.of("openid", "some-scope");
						}

						@Override
						public String getClientName() {
							return "client";
						}
					};
					return Map.of("oauth2-client", registration);
				}

				@Override
				public Map<String, Provider> getProviders() {
					return Collections.emptyMap();
				}
			};
		}

	}

}
