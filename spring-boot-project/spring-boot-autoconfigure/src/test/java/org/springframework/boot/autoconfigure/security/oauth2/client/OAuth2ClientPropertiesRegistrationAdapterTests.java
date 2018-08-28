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
import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.LoginClientRegistration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties.Provider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
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

	private MockWebServer server;

	@After
	public void cleanup() throws Exception {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void getClientRegistrationsWhenUsingDefinedProviderShouldAdapt() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Provider provider = new Provider();
		provider.setAuthorizationUri("http://example.com/auth");
		provider.setTokenUri("http://example.com/token");
		provider.setUserInfoUri("http://example.com/info");
		provider.setUserInfoAuthenticationMethod("form");
		provider.setUserNameAttribute("sub");
		provider.setJwkSetUri("http://example.com/jwk");
		OAuth2ClientProperties.LoginClientRegistration login = new OAuth2ClientProperties.LoginClientRegistration();
		login.setProvider("provider");
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		login.setClientAuthenticationMethod("post");
		login.setAuthorizationGrantType("authorization_code");
		login.setRedirectUri("http://example.com/redirect");
		login.setScope(Collections.singleton("scope"));
		login.setClientName("clientName");
		properties.getRegistration().getLogin().put("registration", login);
		properties.getProvider().put("provider", provider);
		Map<String, ClientRegistration> registrations = OAuth2ClientPropertiesRegistrationAdapter
				.getClientRegistrations(properties);
		ClientRegistration adapted = registrations.get("registration");
		ProviderDetails adaptedProvider = adapted.getProviderDetails();
		assertThat(adaptedProvider.getAuthorizationUri())
				.isEqualTo("http://example.com/auth");
		assertThat(adaptedProvider.getTokenUri()).isEqualTo("http://example.com/token");
		assertThat(adaptedProvider.getUserInfoEndpoint().getUri())
				.isEqualTo("http://example.com/info");
		assertThat(adaptedProvider.getUserInfoEndpoint().getAuthenticationMethod())
				.isEqualTo(
						org.springframework.security.oauth2.core.AuthenticationMethod.FORM);
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
		OAuth2ClientProperties.LoginClientRegistration login = new OAuth2ClientProperties.LoginClientRegistration();
		login.setProvider("google");
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		properties.getRegistration().getLogin().put("registration", login);
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
				.isEqualTo("{baseUrl}/{action}/oauth2/code/{registrationId}");
		assertThat(adapted.getScopes()).containsExactly("openid", "profile", "email");
		assertThat(adapted.getClientName()).isEqualTo("Google");
	}

	@Test
	public void getClientRegistrationsWhenUsingCommonProviderWithOverrideShouldAdapt() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		OAuth2ClientProperties.LoginClientRegistration login = new OAuth2ClientProperties.LoginClientRegistration();
		login.setProvider("google");
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		login.setClientAuthenticationMethod("post");
		login.setAuthorizationGrantType("authorization_code");
		login.setRedirectUri("http://example.com/redirect");
		login.setScope(Collections.singleton("scope"));
		login.setClientName("clientName");
		properties.getRegistration().getLogin().put("registration", login);
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
		assertThat(adaptedProvider.getUserInfoEndpoint().getAuthenticationMethod())
				.isEqualTo(
						org.springframework.security.oauth2.core.AuthenticationMethod.HEADER);
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
		OAuth2ClientProperties.LoginClientRegistration login = new OAuth2ClientProperties.LoginClientRegistration();
		login.setProvider("missing");
		properties.getRegistration().getLogin().put("registration", login);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Unknown provider ID 'missing'");
		OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties);
	}

	@Test
	public void getClientRegistrationsWhenProviderNotSpecifiedShouldUseRegistrationId() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		OAuth2ClientProperties.LoginClientRegistration login = new OAuth2ClientProperties.LoginClientRegistration();
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		properties.getRegistration().getLogin().put("google", login);
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
		assertThat(adaptedProvider.getUserInfoEndpoint().getAuthenticationMethod())
				.isEqualTo(
						org.springframework.security.oauth2.core.AuthenticationMethod.HEADER);
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
				.isEqualTo("{baseUrl}/{action}/oauth2/code/{registrationId}");
		assertThat(adapted.getScopes()).containsExactly("openid", "profile", "email");
		assertThat(adapted.getClientName()).isEqualTo("Google");
	}

	@Test
	public void getClientRegistrationsWhenAuhtorizationCodeClientShouldAdapt() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		OAuth2ClientProperties.AuthorizationCodeClientRegistration registration = new OAuth2ClientProperties.AuthorizationCodeClientRegistration();
		registration.setClientId("clientId");
		registration.setClientSecret("clientSecret");
		registration.setRedirectUri("http://my-redirect-uri.com");
		properties.getRegistration().getAuthorizationCode().put("google", registration);
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
		assertThat(adaptedProvider.getUserInfoEndpoint().getAuthenticationMethod())
				.isEqualTo(
						org.springframework.security.oauth2.core.AuthenticationMethod.HEADER);
		assertThat(adaptedProvider.getJwkSetUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
		assertThat(adapted.getRegistrationId()).isEqualTo("google");
		assertThat(adapted.getClientId()).isEqualTo("clientId");
		assertThat(adapted.getClientSecret()).isEqualTo("clientSecret");
		assertThat(adapted.getRedirectUriTemplate())
				.isEqualTo("http://my-redirect-uri.com");
		assertThat(adapted.getClientAuthenticationMethod()).isEqualTo(
				org.springframework.security.oauth2.core.ClientAuthenticationMethod.BASIC);
		assertThat(adapted.getAuthorizationGrantType()).isEqualTo(
				org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getScopes()).containsExactly("openid", "profile", "email");
		assertThat(adapted.getClientName()).isEqualTo("Google");
	}

	@Test
	public void getClientRegistrationsWhenProviderNotSpecifiedAndUnknownProviderShouldThrowException() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		OAuth2ClientProperties.LoginClientRegistration login = new OAuth2ClientProperties.LoginClientRegistration();
		properties.getRegistration().getLogin().put("missing", login);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(
				"Provider ID must be specified for client registration 'missing'");
		OAuth2ClientPropertiesRegistrationAdapter.getClientRegistrations(properties);
	}

	@Test
	public void oidcProviderConfigurationWhenProviderNotSpecifiedOnRegistration()
			throws Exception {
		LoginClientRegistration login = new OAuth2ClientProperties.LoginClientRegistration();
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		testOidcConfiguration(login, "okta");
	}

	@Test
	public void oidcProviderConfigurationWhenProviderSpecifiedOnRegistration()
			throws Exception {
		OAuth2ClientProperties.LoginClientRegistration login = new LoginClientRegistration();
		login.setProvider("okta-oidc");
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		testOidcConfiguration(login, "okta-oidc");
	}

	@Test
	public void oidcProviderConfigurationWithCustomConfigurationOverridesProviderDefaults()
			throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		OAuth2ClientProperties.LoginClientRegistration login = new OAuth2ClientProperties.LoginClientRegistration();
		login.setProvider("okta-oidc");
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		login.setClientAuthenticationMethod("post");
		login.setRedirectUri("http://example.com/redirect");
		login.setScope(Collections.singleton("user"));
		Provider provider = new Provider();
		provider.setIssuerUri(issuer);
		provider.setAuthorizationUri("http://example.com/auth");
		provider.setTokenUri("http://example.com/token");
		provider.setUserInfoUri("http://example.com/info");
		provider.setUserNameAttribute("sub");
		provider.setJwkSetUri("http://example.com/jwk");
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		properties.getProvider().put("okta-oidc", provider);
		properties.getRegistration().getLogin().put("okta", login);
		Map<String, ClientRegistration> registrations = OAuth2ClientPropertiesRegistrationAdapter
				.getClientRegistrations(properties);
		ClientRegistration adapted = registrations.get("okta");
		ProviderDetails providerDetails = adapted.getProviderDetails();
		assertThat(adapted.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.POST);
		assertThat(adapted.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRegistrationId()).isEqualTo("okta");
		assertThat(adapted.getClientName()).isEqualTo(cleanIssuerPath);
		assertThat(adapted.getScopes()).containsOnly("user");
		assertThat(adapted.getRedirectUriTemplate())
				.isEqualTo("http://example.com/redirect");
		assertThat(providerDetails.getAuthorizationUri())
				.isEqualTo("http://example.com/auth");
		assertThat(providerDetails.getTokenUri()).isEqualTo("http://example.com/token");
		assertThat(providerDetails.getJwkSetUri()).isEqualTo("http://example.com/jwk");
		assertThat(providerDetails.getUserInfoEndpoint().getUri())
				.isEqualTo("http://example.com/info");
		assertThat(providerDetails.getUserInfoEndpoint().getUserNameAttributeName())
				.isEqualTo("sub");
	}

	private void testOidcConfiguration(
			OAuth2ClientProperties.LoginClientRegistration registration,
			String providerId) throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Provider provider = new Provider();
		provider.setIssuerUri(issuer);
		properties.getProvider().put(providerId, provider);
		properties.getRegistration().getLogin().put("okta", registration);
		Map<String, ClientRegistration> registrations = OAuth2ClientPropertiesRegistrationAdapter
				.getClientRegistrations(properties);
		ClientRegistration adapted = registrations.get("okta");
		ProviderDetails providerDetails = adapted.getProviderDetails();
		assertThat(adapted.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.BASIC);
		assertThat(adapted.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRegistrationId()).isEqualTo("okta");
		assertThat(adapted.getClientName()).isEqualTo(cleanIssuerPath);
		assertThat(adapted.getScopes()).containsOnly("openid");
		assertThat(providerDetails.getAuthorizationUri())
				.isEqualTo("https://example.com/o/oauth2/v2/auth");
		assertThat(providerDetails.getTokenUri())
				.isEqualTo("https://example.com/oauth2/v4/token");
		assertThat(providerDetails.getJwkSetUri())
				.isEqualTo("https://example.com/oauth2/v3/certs");
		assertThat(providerDetails.getUserInfoEndpoint().getUri())
				.isEqualTo("https://example.com/oauth2/v3/userinfo");
		assertThat(providerDetails.getUserInfoEndpoint().getAuthenticationMethod())
				.isEqualTo(
						org.springframework.security.oauth2.core.AuthenticationMethod.HEADER);
	}

	private String cleanIssuerPath(String issuer) {
		if (issuer.endsWith("/")) {
			return issuer.substring(0, issuer.length() - 1);
		}
		return issuer;
	}

	private void setupMockResponse(String issuer) throws Exception {
		MockResponse mockResponse = new MockResponse()
				.setResponseCode(HttpStatus.OK.value())
				.setBody(new ObjectMapper().writeValueAsString(getResponse(issuer)))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		this.server.enqueue(mockResponse);
	}

	private Map<String, Object> getResponse(String issuer) {
		Map<String, Object> response = new HashMap<>();
		response.put("authorization_endpoint", "https://example.com/o/oauth2/v2/auth");
		response.put("claims_supported", Collections.emptyList());
		response.put("code_challenge_methods_supported", Collections.emptyList());
		response.put("id_token_signing_alg_values_supported", Collections.emptyList());
		response.put("issuer", issuer);
		response.put("jwks_uri", "https://example.com/oauth2/v3/certs");
		response.put("response_types_supported", Collections.emptyList());
		response.put("revocation_endpoint", "https://example.com/o/oauth2/revoke");
		response.put("scopes_supported", Collections.singletonList("openid"));
		response.put("subject_types_supported", Collections.singletonList("public"));
		response.put("grant_types_supported",
				Collections.singletonList("authorization_code"));
		response.put("token_endpoint", "https://example.com/oauth2/v4/token");
		response.put("token_endpoint_auth_methods_supported",
				Collections.singletonList("client_secret_basic"));
		response.put("userinfo_endpoint", "https://example.com/oauth2/v3/userinfo");
		return response;
	}

}
