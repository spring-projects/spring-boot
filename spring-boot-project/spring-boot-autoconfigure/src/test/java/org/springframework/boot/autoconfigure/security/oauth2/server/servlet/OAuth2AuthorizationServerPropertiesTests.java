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

import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link OAuth2AuthorizationServerProperties}.
 *
 * @author Steve Riesenberg
 */
class OAuth2AuthorizationServerPropertiesTests {

	private final OAuth2AuthorizationServerProperties properties = new OAuth2AuthorizationServerProperties();

	@Test
	void clientIdAbsentThrowsException() {
		OAuth2AuthorizationServerProperties.Client client = new OAuth2AuthorizationServerProperties.Client();
		client.getRegistration().getClientAuthenticationMethods().add("client_secret_basic");
		client.getRegistration().getAuthorizationGrantTypes().add("authorization_code");
		this.properties.getClient().put("foo", client);
		assertThatIllegalStateException().isThrownBy(this.properties::validate)
			.withMessage("Client id must not be empty.");
	}

	@Test
	void clientSecretAbsentShouldNotThrowException() {
		OAuth2AuthorizationServerProperties.Client client = new OAuth2AuthorizationServerProperties.Client();
		client.getRegistration().setClientId("foo");
		client.getRegistration().getClientAuthenticationMethods().add("client_secret_basic");
		client.getRegistration().getAuthorizationGrantTypes().add("authorization_code");
		this.properties.getClient().put("foo", client);
		this.properties.validate();
	}

	@Test
	void clientAuthenticationMethodsEmptyThrowsException() {
		OAuth2AuthorizationServerProperties.Client client = new OAuth2AuthorizationServerProperties.Client();
		client.getRegistration().setClientId("foo");
		client.getRegistration().getAuthorizationGrantTypes().add("authorization_code");
		this.properties.getClient().put("foo", client);
		assertThatIllegalStateException().isThrownBy(this.properties::validate)
			.withMessage("Client authentication methods must not be empty.");
	}

	@Test
	void authorizationGrantTypesEmptyThrowsException() {
		OAuth2AuthorizationServerProperties.Client client = new OAuth2AuthorizationServerProperties.Client();
		client.getRegistration().setClientId("foo");
		client.getRegistration().getClientAuthenticationMethods().add("client_secret_basic");
		this.properties.getClient().put("foo", client);
		assertThatIllegalStateException().isThrownBy(this.properties::validate)
			.withMessage("Authorization grant types must not be empty.");
	}

	@Test
	void defaultDeviceCodeTimeToLiveMatchesBuilderDefault() {
		assertThat(new OAuth2AuthorizationServerProperties.Client().getToken().getDeviceCodeTimeToLive())
			.isEqualTo(TokenSettings.builder().build().getDeviceCodeTimeToLive());
	}

}
