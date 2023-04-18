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

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2AuthorizationServerPropertiesMapper}.
 *
 * @author Steve Riesenberg
 */
class OAuth2AuthorizationServerPropertiesMapperTests {

	private final OAuth2AuthorizationServerProperties properties = new OAuth2AuthorizationServerProperties();

	private final OAuth2AuthorizationServerPropertiesMapper mapper = new OAuth2AuthorizationServerPropertiesMapper(
			this.properties);

	@Test
	void getRegisteredClientsWhenValidParametersShouldAdapt() {
		OAuth2AuthorizationServerProperties.Client client = createClient();
		this.properties.getClient().put("foo", client);
		List<RegisteredClient> registeredClients = this.mapper.asRegisteredClients();
		assertThat(registeredClients).hasSize(1);
		RegisteredClient registeredClient = registeredClients.get(0);
		assertThat(registeredClient.getClientId()).isEqualTo("foo");
		assertThat(registeredClient.getClientSecret()).isEqualTo("secret");
		assertThat(registeredClient.getClientAuthenticationMethods())
			.containsExactly(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
		assertThat(registeredClient.getAuthorizationGrantTypes())
			.containsExactly(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(registeredClient.getRedirectUris()).containsExactly("https://example.com/redirect");
		assertThat(registeredClient.getPostLogoutRedirectUris()).containsExactly("https://example.com/logout");
		assertThat(registeredClient.getScopes()).containsExactly("user.read");
		assertThat(registeredClient.getClientSettings().isRequireProofKey()).isTrue();
		assertThat(registeredClient.getClientSettings().isRequireAuthorizationConsent()).isTrue();
		assertThat(registeredClient.getClientSettings().getJwkSetUrl()).isEqualTo("https://example.com/jwks");
		assertThat(registeredClient.getClientSettings().getTokenEndpointAuthenticationSigningAlgorithm())
			.isEqualTo(SignatureAlgorithm.RS256);
		assertThat(registeredClient.getTokenSettings().getAccessTokenFormat()).isEqualTo(OAuth2TokenFormat.REFERENCE);
		assertThat(registeredClient.getTokenSettings().getAccessTokenTimeToLive()).isEqualTo(Duration.ofSeconds(300));
		assertThat(registeredClient.getTokenSettings().getRefreshTokenTimeToLive()).isEqualTo(Duration.ofHours(24));
		assertThat(registeredClient.getTokenSettings().getDeviceCodeTimeToLive()).isEqualTo(Duration.ofMinutes(30));
		assertThat(registeredClient.getTokenSettings().isReuseRefreshTokens()).isEqualTo(true);
		assertThat(registeredClient.getTokenSettings().getIdTokenSignatureAlgorithm())
			.isEqualTo(SignatureAlgorithm.RS512);
	}

	private OAuth2AuthorizationServerProperties.Client createClient() {
		OAuth2AuthorizationServerProperties.Client client = new OAuth2AuthorizationServerProperties.Client();
		client.setRequireProofKey(true);
		client.setRequireAuthorizationConsent(true);
		client.setJwkSetUri("https://example.com/jwks");
		client.setTokenEndpointAuthenticationSigningAlgorithm("rs256");
		OAuth2AuthorizationServerProperties.Registration registration = client.getRegistration();
		registration.setClientId("foo");
		registration.setClientSecret("secret");
		registration.getClientAuthenticationMethods().add("client_secret_basic");
		registration.getAuthorizationGrantTypes().add("authorization_code");
		registration.getRedirectUris().add("https://example.com/redirect");
		registration.getPostLogoutRedirectUris().add("https://example.com/logout");
		registration.getScopes().add("user.read");
		OAuth2AuthorizationServerProperties.Token token = client.getToken();
		token.setAccessTokenFormat("reference");
		token.setAccessTokenTimeToLive(Duration.ofSeconds(300));
		token.setRefreshTokenTimeToLive(Duration.ofHours(24));
		token.setDeviceCodeTimeToLive(Duration.ofMinutes(30));
		token.setReuseRefreshTokens(true);
		token.setIdTokenSignatureAlgorithm("rs512");
		return client;
	}

	@Test
	void getAuthorizationServerSettingsWhenValidParametersShouldAdapt() {
		this.properties.setIssuer("https://example.com");
		OAuth2AuthorizationServerProperties.Endpoint endpoints = this.properties.getEndpoint();
		endpoints.setAuthorizationUri("/authorize");
		endpoints.setDeviceAuthorizationUri("/device_authorization");
		endpoints.setDeviceVerificationUri("/device_verification");
		endpoints.setTokenUri("/token");
		endpoints.setJwkSetUri("/jwks");
		endpoints.setTokenRevocationUri("/revoke");
		endpoints.setTokenIntrospectionUri("/introspect");
		OAuth2AuthorizationServerProperties.OidcEndpoint oidc = endpoints.getOidc();
		oidc.setLogoutUri("/logout");
		oidc.setClientRegistrationUri("/register");
		oidc.setUserInfoUri("/user");
		AuthorizationServerSettings settings = this.mapper.asAuthorizationServerSettings();
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
	}

}
