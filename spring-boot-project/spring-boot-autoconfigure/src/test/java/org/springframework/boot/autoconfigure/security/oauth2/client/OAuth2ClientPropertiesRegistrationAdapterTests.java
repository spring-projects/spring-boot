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

import java.util.Collections;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Registration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2ClientPropertiesRegistrationAdapter}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Thiago Hirata
 */
public class OAuth2ClientPropertiesRegistrationAdapterTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void getClientRegistrationsWhenUsingDefinedProviderShouldAdapt() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Provider provider = new Provider();
		provider.setAuthorizationUri("http://example.com/auth");
		provider.setTokenUri("http://example.com/token");
		provider.setUserInfoUri("http://example.com/info");
		provider.setUserNameAttribute("sub");
		provider.setJwkSetUri("http://example.com/jwk");
		Registration registration = new Registration();
		registration.setProvider("provider");
		registration.setClientId("clientId");
		registration.setClientSecret("clientSecret");
		registration.setClientAuthenticationMethod("post");
		registration.setAuthorizationGrantType("authorization_code");
		registration.setRedirectUriTemplate("http://example.com/redirect");
		registration.setScope(Collections.singleton("scope"));
		registration.setClientName("clientName");
		properties.getProvider().put("provider", provider);
		properties.getRegistration().put("registration", registration);
		Map<String, ClientRegistration> registrations = OAuth2ClientPropertiesRegistrationAdapter
				.getClientRegistrations(properties);
		ClientRegistration adapted = registrations.get("registration");
		ProviderDetails adaptedProvider = adapted.getProviderDetails();
		assertThat(adaptedProvider.getAuthorizationUri())
				.isEqualTo("http://example.com/auth");
		assertThat(adaptedProvider.getTokenUri()).isEqualTo("http://example.com/token");
		assertThat(adaptedProvider.getUserInfoEndpoint().getUri())
				.isEqualTo("http://example.com/info");
		assertThat(adaptedProvider.getUserInfoEndpoint().getUserNameAttributeName())
				.isEqualTo("sub");
		assertThat(adaptedProvider.getJwkSetUri()).isEqualTo("http://example.com/jwk");
		assertThat(adapted.getRegistrationId()).isEqualTo("registration");
		assertThat(adapted.getClientId()).isEqualTo("clientId");
		assertThat(adapted.getClientSecret()).isEqualTo("clientSecret");
		assertThat(adapted.getClientAuthenticationMethod()).isEqualTo(
				org.springframework.security.oauth2.core.ClientAuthenticationMethod.POST);
		assertThat(adapted.getAuthorizationGrantType()).isEqualTo(
				org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRedirectUriTemplate())
				.isEqualTo("http://example.com/redirect");
		assertThat(adapted.getScopes()).containsExactly("scope");
		assertThat(adapted.getClientName()).isEqualTo("clientName");
	}

	@Test
	public void getClientRegistrationsWhenUsingCommonProviderShouldAdapt() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Registration registration = new Registration();
		registration.setProvider("google");
		registration.setClientId("clientId");
		registration.setClientSecret("clientSecret");
		properties.getRegistration().put("registration", registration);
		Map<String, ClientRegistration> registrations = OAuth2ClientPropertiesRegistrationAdapter
				.getClientRegistrations(properties);
		ClientRegistration adapted = registrations.get("registration");
		ProviderDetails adaptedProvider = adapted.getProviderDetails();
		assertThat(adaptedProvider.getAuthorizationUri())
				.isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(adaptedProvider.getTokenUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v4/token");
		assertThat(adaptedProvider.getUserInfoEndpoint().getUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/userinfo");
		assertThat(adaptedProvider.getUserInfoEndpoint().getUserNameAttributeName())
				.isEqualTo(IdTokenClaimNames.SUB);
		assertThat(adaptedProvider.getJwkSetUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
		assertThat(adapted.getRegistrationId()).isEqualTo("registration");
		assertThat(adapted.getClientId()).isEqualTo("clientId");
		assertThat(adapted.getClientSecret()).isEqualTo("clientSecret");
		assertThat(adapted.getClientAuthenticationMethod()).isEqualTo(
				org.springframework.security.oauth2.core.ClientAuthenticationMethod.BASIC);
		assertThat(adapted.getAuthorizationGrantType()).isEqualTo(
				org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRedirectUriTemplate())
				.isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
		assertThat(adapted.getScopes()).containsExactly("openid", "profile", "email");
		assertThat(adapted.getClientName()).isEqualTo("Google");
	}

	@Test
	public void getClientRegistrationsWhenUsingCommonProviderWithOverrideShouldAdapt() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Registration registration = new Registration();
		registration.setProvider("google");
		registration.setClientId("clientId");
		registration.setClientSecret("clientSecret");
		registration.setClientAuthenticationMethod("post");
		registration.setAuthorizationGrantType("authorization_code");
		registration.setRedirectUriTemplate("http://example.com/redirect");
		registration.setScope(Collections.singleton("scope"));
		registration.setClientName("clientName");
		properties.getRegistration().put("registration", registration);
		Map<String, ClientRegistration> registrations = OAuth2ClientPropertiesRegistrationAdapter
				.getClientRegistrations(properties);
		ClientRegistration adapted = registrations.get("registration");
		ProviderDetails adaptedProvider = adapted.getProviderDetails();
		assertThat(adaptedProvider.getAuthorizationUri())
				.isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(adaptedProvider.getTokenUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v4/token");
		assertThat(adaptedProvider.getUserInfoEndpoint().getUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/userinfo");
		assertThat(adaptedProvider.getUserInfoEndpoint().getUserNameAttributeName())
				.isEqualTo(IdTokenClaimNames.SUB);
		assertThat(adaptedProvider.getJwkSetUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
		assertThat(adapted.getRegistrationId()).isEqualTo("registration");
		assertThat(adapted.getClientId()).isEqualTo("clientId");
		assertThat(adapted.getClientSecret()).isEqualTo("clientSecret");
		assertThat(adapted.getClientAuthenticationMethod()).isEqualTo(
				org.springframework.security.oauth2.core.ClientAuthenticationMethod.POST);
		assertThat(adapted.getAuthorizationGrantType()).isEqualTo(
				org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRedirectUriTemplate())
				.isEqualTo("http://example.com/redirect");
		assertThat(adapted.getScopes()).containsExactly("scope");
		assertThat(adapted.getClientName()).isEqualTo("clientName");
	}

	@Test
	public void getClientRegistrationsWhenUnknownProviderShouldThrowException() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Registration registration = new Registration();
		registration.setProvider("missing");
		properties.getRegistration().put("registration", registration);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unknown provider ID 'missing'");
		OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties);
	}

	@Test
	public void getClientRegistrationsWhenProviderNotSpecifiedShouldUseRegistrationId() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Registration registration = new Registration();
		registration.setClientId("clientId");
		registration.setClientSecret("clientSecret");
		properties.getRegistration().put("google", registration);
		Map<String, ClientRegistration> registrations = OAuth2ClientPropertiesRegistrationAdapter
				.getClientRegistrations(properties);
		ClientRegistration adapted = registrations.get("google");
		ProviderDetails adaptedProvider = adapted.getProviderDetails();
		assertThat(adaptedProvider.getAuthorizationUri())
				.isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(adaptedProvider.getTokenUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v4/token");
		assertThat(adaptedProvider.getUserInfoEndpoint().getUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/userinfo");
		assertThat(adaptedProvider.getJwkSetUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
		assertThat(adapted.getRegistrationId()).isEqualTo("google");
		assertThat(adapted.getClientId()).isEqualTo("clientId");
		assertThat(adapted.getClientSecret()).isEqualTo("clientSecret");
		assertThat(adapted.getClientAuthenticationMethod()).isEqualTo(
				org.springframework.security.oauth2.core.ClientAuthenticationMethod.BASIC);
		assertThat(adapted.getAuthorizationGrantType()).isEqualTo(
				org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRedirectUriTemplate())
				.isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}");
		assertThat(adapted.getScopes()).containsExactly("openid", "profile", "email");
		assertThat(adapted.getClientName()).isEqualTo("Google");
	}

	@Test
	public void getClientRegistrationsWhenProviderNotSpecifiedAndUnknownProviderShouldThrowException() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Registration registration = new Registration();
		properties.getRegistration().put("missing", registration);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(
				"Provider ID must be specified for client registration 'missing'");
		OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties);
	}

}
