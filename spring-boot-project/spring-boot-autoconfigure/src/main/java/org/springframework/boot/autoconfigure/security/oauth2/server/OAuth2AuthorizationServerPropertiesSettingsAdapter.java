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

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

/**
 * Adapter class to convert {@link OAuth2AuthorizationServerProperties.Endpoint} to a
 * {@link AuthorizationServerSettings}.
 *
 * @author Steve Riesenberg
 * @since 3.1.0
 */
public final class OAuth2AuthorizationServerPropertiesSettingsAdapter {

	private OAuth2AuthorizationServerPropertiesSettingsAdapter() {
	}

	public static AuthorizationServerSettings getAuthorizationServerSettings(
			OAuth2AuthorizationServerProperties properties) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		OAuth2AuthorizationServerProperties.Endpoint endpoint = properties.getEndpoint();
		OAuth2AuthorizationServerProperties.OidcEndpoint oidc = endpoint.getOidc();
		AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();
		map.from(properties::getIssuer).to(builder::issuer);
		map.from(endpoint::getAuthorizationUri).to(builder::authorizationEndpoint);
		map.from(endpoint::getTokenUri).to(builder::tokenEndpoint);
		map.from(endpoint::getJwkSetUri).to(builder::jwkSetEndpoint);
		map.from(endpoint::getTokenRevocationUri).to(builder::tokenRevocationEndpoint);
		map.from(endpoint::getTokenIntrospectionUri).to(builder::tokenIntrospectionEndpoint);
		map.from(oidc::getLogoutUri).to(builder::oidcLogoutEndpoint);
		map.from(oidc::getClientRegistrationUri).to(builder::oidcClientRegistrationEndpoint);
		map.from(oidc::getUserInfoUri).to(builder::oidcUserInfoEndpoint);
		return builder.build();
	}

}
