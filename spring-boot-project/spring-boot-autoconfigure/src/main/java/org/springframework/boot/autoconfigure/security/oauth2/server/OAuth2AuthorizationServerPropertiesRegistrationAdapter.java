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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.security.oauth2.server.OAuth2AuthorizationServerProperties.Client;
import org.springframework.boot.autoconfigure.security.oauth2.server.OAuth2AuthorizationServerProperties.Registration;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.CollectionUtils;

/**
 * Adapter class to convert {@link Client} to a {@link RegisteredClient}.
 *
 * @author Steve Riesenberg
 * @since 3.1.0
 */
public final class OAuth2AuthorizationServerPropertiesRegistrationAdapter {

	private OAuth2AuthorizationServerPropertiesRegistrationAdapter() {
	}

	public static List<RegisteredClient> getRegisteredClients(OAuth2AuthorizationServerProperties properties) {
		List<RegisteredClient> registeredClients = new ArrayList<>();
		properties.getClient()
			.forEach((registrationId, client) -> registeredClients.add(getRegisteredClient(registrationId, client)));
		return registeredClients;
	}

	private static RegisteredClient getRegisteredClient(String registrationId, Client client) {
		Registration registration = client.getRegistration();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		RegisteredClient.Builder builder = RegisteredClient.withId(registrationId);
		map.from(registration::getClientId).to(builder::clientId);
		map.from(registration::getClientSecret).to(builder::clientSecret);
		map.from(registration::getClientName).to(builder::clientName);
		if (!CollectionUtils.isEmpty(registration.getClientAuthenticationMethods())) {
			registration.getClientAuthenticationMethods()
				.forEach((clientAuthenticationMethod) -> map.from(clientAuthenticationMethod)
					.as(OAuth2AuthorizationServerPropertiesRegistrationAdapter::clientAuthenticationMethod)
					.to(builder::clientAuthenticationMethod));
		}
		if (!CollectionUtils.isEmpty(registration.getAuthorizationGrantTypes())) {
			registration.getAuthorizationGrantTypes()
				.forEach((authorizationGrantType) -> map.from(authorizationGrantType)
					.as(OAuth2AuthorizationServerPropertiesRegistrationAdapter::authorizationGrantType)
					.to(builder::authorizationGrantType));
		}
		if (!CollectionUtils.isEmpty(registration.getRedirectUris())) {
			registration.getRedirectUris().forEach((redirectUri) -> map.from(redirectUri).to(builder::redirectUri));
		}
		if (!CollectionUtils.isEmpty(registration.getPostLogoutRedirectUris())) {
			registration.getPostLogoutRedirectUris()
				.forEach((redirectUri) -> map.from(redirectUri).to(builder::postLogoutRedirectUri));
		}
		if (!CollectionUtils.isEmpty(registration.getScopes())) {
			registration.getScopes().forEach((scope) -> map.from(scope).to(builder::scope));
		}
		builder.clientSettings(getClientSettings(client, map));
		builder.tokenSettings(getTokenSettings(client, map));
		return builder.build();
	}

	private static ClientAuthenticationMethod clientAuthenticationMethod(String clientAuthenticationMethod) {
		if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equals(clientAuthenticationMethod)) {
			return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
		}
		else if (ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue().equals(clientAuthenticationMethod)) {
			return ClientAuthenticationMethod.CLIENT_SECRET_POST;
		}
		else if (ClientAuthenticationMethod.CLIENT_SECRET_JWT.getValue().equals(clientAuthenticationMethod)) {
			return ClientAuthenticationMethod.CLIENT_SECRET_JWT;
		}
		else if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.getValue().equals(clientAuthenticationMethod)) {
			return ClientAuthenticationMethod.PRIVATE_KEY_JWT;
		}
		else if (ClientAuthenticationMethod.NONE.getValue().equals(clientAuthenticationMethod)) {
			return ClientAuthenticationMethod.NONE;
		}
		else {
			return new ClientAuthenticationMethod(clientAuthenticationMethod);
		}
	}

	private static AuthorizationGrantType authorizationGrantType(String authorizationGrantType) {
		if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
			return AuthorizationGrantType.AUTHORIZATION_CODE;
		}
		else if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(authorizationGrantType)) {
			return AuthorizationGrantType.CLIENT_CREDENTIALS;
		}
		else {
			return new AuthorizationGrantType(authorizationGrantType);
		}
	}

	private static ClientSettings getClientSettings(Client client, PropertyMapper map) {
		ClientSettings.Builder builder = ClientSettings.builder();
		map.from(client::isRequireProofKey).to(builder::requireProofKey);
		map.from(client::isRequireAuthorizationConsent).to(builder::requireAuthorizationConsent);
		map.from(client::getJwkSetUri).to(builder::jwkSetUrl);
		map.from(client::getTokenEndpointAuthenticationSigningAlgorithm)
			.as(OAuth2AuthorizationServerPropertiesRegistrationAdapter::jwsAlgorithm)
			.to(builder::tokenEndpointAuthenticationSigningAlgorithm);
		return builder.build();
	}

	private static TokenSettings getTokenSettings(Client client, PropertyMapper map) {
		OAuth2AuthorizationServerProperties.Token token = client.getToken();
		TokenSettings.Builder builder = TokenSettings.builder();
		map.from(token::getAuthorizationCodeTimeToLive).to(builder::authorizationCodeTimeToLive);
		map.from(token::getAccessTokenTimeToLive).to(builder::accessTokenTimeToLive);
		map.from(token::getAccessTokenFormat).as(OAuth2TokenFormat::new).to(builder::accessTokenFormat);
		map.from(token::isReuseRefreshTokens).to(builder::reuseRefreshTokens);
		map.from(token::getRefreshTokenTimeToLive).to(builder::refreshTokenTimeToLive);
		map.from(token::getIdTokenSignatureAlgorithm)
			.as(OAuth2AuthorizationServerPropertiesRegistrationAdapter::signatureAlgorithm)
			.to(builder::idTokenSignatureAlgorithm);
		return builder.build();
	}

	private static JwsAlgorithm jwsAlgorithm(String signingAlgorithm) {
		String name = signingAlgorithm.toUpperCase();
		JwsAlgorithm jwsAlgorithm = SignatureAlgorithm.from(name);
		if (jwsAlgorithm == null) {
			jwsAlgorithm = MacAlgorithm.from(name);
		}
		return jwsAlgorithm;
	}

	private static SignatureAlgorithm signatureAlgorithm(String signatureAlgorithm) {
		return SignatureAlgorithm.from(signatureAlgorithm.toUpperCase());
	}

}
