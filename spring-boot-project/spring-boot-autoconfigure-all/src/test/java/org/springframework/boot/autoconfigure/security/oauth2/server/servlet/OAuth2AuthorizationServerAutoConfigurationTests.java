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

package org.springframework.boot.autoconfigure.security.oauth2.server.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link OAuth2AuthorizationServerAutoConfiguration}.
 *
 * @author Steve Riesenberg
 * @author Madhura Bhave
 */
class OAuth2AuthorizationServerAutoConfigurationTests {

	private static final String PROPERTIES_PREFIX = "spring.security.oauth2.authorizationserver";

	private static final String CLIENT_PREFIX = PROPERTIES_PREFIX + ".client";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OAuth2AuthorizationServerAutoConfiguration.class,
				OAuth2AuthorizationServerJwtAutoConfiguration.class, SecurityAutoConfiguration.class,
				UserDetailsServiceAutoConfiguration.class));

	@Test
	void autoConfigurationConditionalOnClassOauth2Authorization() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(OAuth2Authorization.class))
			.run((context) -> assertThat(context).doesNotHaveBean(OAuth2AuthorizationServerAutoConfiguration.class));
	}

	@Test
	@ClassPathExclusions({ "spring-security-oauth2-client-*.jar", "spring-security-oauth2-resource-server-*.jar",
			"spring-security-saml2-service-provider-*.jar" })
	void autoConfigurationDoesNotCauseUserDetailsServiceToBackOff() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(UserDetailsServiceAutoConfiguration.class)
			.hasBean("inMemoryUserDetailsManager"));
	}

	@Test
	void registeredClientRepositoryBeanShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(RegisteredClientRepository.class));
	}

	@Test
	void registeredClientRepositoryBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner
			.withPropertyValues(CLIENT_PREFIX + ".foo.registration.client-id=abcd",
					CLIENT_PREFIX + ".foo.registration.client-secret=secret",
					CLIENT_PREFIX + ".foo.registration.client-authentication-methods=client_secret_basic",
					CLIENT_PREFIX + ".foo.registration.authorization-grant-types=client_credentials",
					CLIENT_PREFIX + ".foo.registration.scopes=test")
			.run((context) -> {
				RegisteredClientRepository registeredClientRepository = context
					.getBean(RegisteredClientRepository.class);
				RegisteredClient registeredClient = registeredClientRepository.findById("foo");
				assertThat(registeredClient).isNotNull();
				assertThat(registeredClient.getClientId()).isEqualTo("abcd");
				assertThat(registeredClient.getClientSecret()).isEqualTo("secret");
				assertThat(registeredClient.getClientAuthenticationMethods())
					.containsOnly(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
				assertThat(registeredClient.getAuthorizationGrantTypes())
					.containsOnly(AuthorizationGrantType.CLIENT_CREDENTIALS);
				assertThat(registeredClient.getScopes()).containsOnly("test");
			});
	}

	@Test
	void registeredClientRepositoryBacksOffWhenRegisteredClientRepositoryBeanPresent() {
		this.contextRunner.withUserConfiguration(TestRegisteredClientRepositoryConfiguration.class)
			.withPropertyValues(CLIENT_PREFIX + ".foo.registration.client-id=abcd",
					CLIENT_PREFIX + ".foo.registration.client-secret=secret",
					CLIENT_PREFIX + ".foo.registration.client-authentication-methods=client_secret_basic",
					CLIENT_PREFIX + ".foo.registration.authorization-grant-types=client_credentials",
					CLIENT_PREFIX + ".foo.registration.scope=test")
			.run((context) -> {
				RegisteredClientRepository registeredClientRepository = context
					.getBean(RegisteredClientRepository.class);
				RegisteredClient registeredClient = registeredClientRepository.findById("test");
				assertThat(registeredClient).isNotNull();
				assertThat(registeredClient.getClientId()).isEqualTo("abcd");
				assertThat(registeredClient.getClientSecret()).isEqualTo("secret");
				assertThat(registeredClient.getClientAuthenticationMethods())
					.containsOnly(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
				assertThat(registeredClient.getAuthorizationGrantTypes())
					.containsOnly(AuthorizationGrantType.CLIENT_CREDENTIALS);
				assertThat(registeredClient.getScopes()).containsOnly("test");
			});
	}

	@Test
	void authorizationServerSettingsBeanShouldBeCreatedWhenPropertiesAbsent() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(AuthorizationServerSettings.class));
	}

	@Test
	void authorizationServerSettingsBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner
			.withPropertyValues(PROPERTIES_PREFIX + ".issuer=https://example.com",
					PROPERTIES_PREFIX + ".endpoint.authorization-uri=/authorize",
					PROPERTIES_PREFIX + ".endpoint.device-authorization-uri=/device_authorization",
					PROPERTIES_PREFIX + ".endpoint.device-verification-uri=/device_verification",
					PROPERTIES_PREFIX + ".endpoint.token-uri=/token", PROPERTIES_PREFIX + ".endpoint.jwk-set-uri=/jwks",
					PROPERTIES_PREFIX + ".endpoint.token-revocation-uri=/revoke",
					PROPERTIES_PREFIX + ".endpoint.token-introspection-uri=/introspect",
					PROPERTIES_PREFIX + ".endpoint.oidc.logout-uri=/logout",
					PROPERTIES_PREFIX + ".endpoint.oidc.client-registration-uri=/register",
					PROPERTIES_PREFIX + ".endpoint.oidc.user-info-uri=/user")
			.run((context) -> {
				AuthorizationServerSettings settings = context.getBean(AuthorizationServerSettings.class);
				assertThat(settings.getIssuer()).isEqualTo("https://example.com");
				assertThat(settings.getAuthorizationEndpoint()).isEqualTo("/authorize");
				assertThat(settings.getDeviceAuthorizationEndpoint()).isEqualTo("/device_authorization");
				assertThat(settings.getDeviceVerificationEndpoint()).isEqualTo("/device_verification");
				assertThat(settings.getTokenEndpoint()).isEqualTo("/token");
				assertThat(settings.getJwkSetEndpoint()).isEqualTo("/jwks");
				assertThat(settings.getTokenRevocationEndpoint()).isEqualTo("/revoke");
				assertThat(settings.getTokenIntrospectionEndpoint()).isEqualTo("/introspect");
				assertThat(settings.getOidcLogoutEndpoint()).isEqualTo("/logout");
				assertThat(settings.getOidcClientRegistrationEndpoint()).isEqualTo("/register");
				assertThat(settings.getOidcUserInfoEndpoint()).isEqualTo("/user");
			});
	}

	@Test
	void authorizationServerSettingsBacksOffWhenAuthorizationServerSettingsBeanPresent() {
		this.contextRunner.withUserConfiguration(TestAuthorizationServerSettingsConfiguration.class)
			.withPropertyValues(PROPERTIES_PREFIX + ".issuer=https://test.com")
			.run((context) -> {
				AuthorizationServerSettings settings = context.getBean(AuthorizationServerSettings.class);
				assertThat(settings.getIssuer()).isEqualTo("https://example.com");
			});
	}

	@Configuration
	static class TestRegisteredClientRepositoryConfiguration {

		@Bean
		RegisteredClientRepository registeredClientRepository() {
			RegisteredClient registeredClient = RegisteredClient.withId("test")
				.clientId("abcd")
				.clientSecret("secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("test")
				.build();
			return new InMemoryRegisteredClientRepository(registeredClient);
		}

	}

	@Configuration
	static class TestAuthorizationServerSettingsConfiguration {

		@Bean
		AuthorizationServerSettings authorizationServerSettings() {
			return AuthorizationServerSettings.builder().issuer("https://example.com").build();
		}

	}

}
