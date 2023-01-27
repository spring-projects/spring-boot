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

package org.springframework.boot.autoconfigure.security.oauth2.server;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2AuthorizationServerPropertiesRegistrationAdapter}.
 *
 * @author Steve Riesenberg
 */
public class OAuth2AuthorizationServerPropertiesRegistrationAdapterTests {

	@Test
	void getRegisteredClientsWhenValidParametersShouldAdapt() {
		OAuth2AuthorizationServerProperties properties = new OAuth2AuthorizationServerProperties();
		OAuth2AuthorizationServerProperties.Client client = createClient();
		properties.getClient().put("foo", client);

		List<RegisteredClient> registeredClients = OAuth2AuthorizationServerPropertiesRegistrationAdapter
			.getRegisteredClients(properties);
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
		token.setReuseRefreshTokens(true);
		token.setIdTokenSignatureAlgorithm("rs512");

		return client;
	}

}
