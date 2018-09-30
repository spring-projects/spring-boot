/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.AuthorizationCodeClientRegistration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.LoginClientRegistration;

/**
 * Tests for {@link OAuth2ClientProperties}.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 */
public class OAuth2ClientPropertiesTests {

	private OAuth2ClientProperties properties = new OAuth2ClientProperties();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void clientIdAbsentForLoginClientsThrowsException() {
		LoginClientRegistration registration = new LoginClientRegistration();
		registration.setClientSecret("secret");
		registration.setProvider("google");
		this.properties.getRegistration().getLogin().put("foo", registration);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Client id must not be empty.");
		this.properties.validate();
	}

	@Test
	public void clientSecretAbsentShouldNotThrowException() {
		LoginClientRegistration registration = new LoginClientRegistration();
		registration.setClientId("foo");
		registration.setProvider("google");
		this.properties.getRegistration().getLogin().put("foo", registration);
		this.properties.validate();
	}

	@Test
	public void clientIdAbsentForAuthorizationCodeClientsThrowsException() {
		AuthorizationCodeClientRegistration registration = new AuthorizationCodeClientRegistration();
		registration.setClientSecret("secret");
		registration.setProvider("google");
		this.properties.getRegistration().getAuthorizationCode().put("foo", registration);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Client id must not be empty.");
		this.properties.validate();
	}

	@Test
	public void clientSecretAbsentForAuthorizationCodeClientDoesNotThrowException() {
		AuthorizationCodeClientRegistration registration = new AuthorizationCodeClientRegistration();
		registration.setClientId("foo");
		registration.setProvider("google");
		this.properties.getRegistration().getAuthorizationCode().put("foo", registration);
		this.properties.validate();
	}

}
