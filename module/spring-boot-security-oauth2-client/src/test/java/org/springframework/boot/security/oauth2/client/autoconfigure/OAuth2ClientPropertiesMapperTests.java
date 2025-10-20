/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.oauth2.client.autoconfigure;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties.Provider;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties.Registration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails.UserInfoEndpoint;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link OAuth2ClientPropertiesMapper}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Thiago Hirata
 * @author HaiTao Zhang
 */
class OAuth2ClientPropertiesMapperTests {

	private @Nullable MockWebServer server;

	@AfterEach
	void cleanup() throws Exception {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	@Test
	void getClientRegistrationsWhenUsingDefinedProviderShouldAdapt() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Provider provider = createProvider();
		provider.setUserInfoAuthenticationMethod("form");
		OAuth2ClientProperties.Registration registration = createRegistration("provider");
		registration.setClientName("clientName");
		properties.getRegistration().put("registration", registration);
		properties.getProvider().put("provider", provider);
		Map<String, ClientRegistration> registrations = new OAuth2ClientPropertiesMapper(properties)
			.asClientRegistrations();
		ClientRegistration adapted = registrations.get("registration");
		assertThat(adapted).isNotNull();
		ProviderDetails adaptedProvider = adapted.getProviderDetails();
		assertThat(adaptedProvider.getAuthorizationUri()).isEqualTo("https://example.com/auth");
		assertThat(adaptedProvider.getTokenUri()).isEqualTo("https://example.com/token");
		UserInfoEndpoint userInfoEndpoint = adaptedProvider.getUserInfoEndpoint();
		assertThat(userInfoEndpoint.getUri()).isEqualTo("https://example.com/info");
		assertThat(userInfoEndpoint.getAuthenticationMethod())
			.isEqualTo(org.springframework.security.oauth2.core.AuthenticationMethod.FORM);
		assertThat(userInfoEndpoint.getUserNameAttributeName()).isEqualTo("sub");
		assertThat(adaptedProvider.getJwkSetUri()).isEqualTo("https://example.com/jwk");
		assertThat(adapted.getRegistrationId()).isEqualTo("registration");
		assertThat(adapted.getClientId()).isEqualTo("clientId");
		assertThat(adapted.getClientSecret()).isEqualTo("clientSecret");
		assertThat(adapted.getClientAuthenticationMethod())
			.isEqualTo(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST);
		assertThat(adapted.getAuthorizationGrantType())
			.isEqualTo(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRedirectUri()).isEqualTo("https://example.com/redirect");
		assertThat(adapted.getScopes()).containsExactly("user");
		assertThat(adapted.getClientName()).isEqualTo("clientName");
	}

	@Test
	void getClientRegistrationsWhenUsingCommonProviderShouldAdapt() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
		registration.setProvider("google");
		registration.setClientId("clientId");
		registration.setClientSecret("clientSecret");
		properties.getRegistration().put("registration", registration);
		Map<String, ClientRegistration> registrations = new OAuth2ClientPropertiesMapper(properties)
			.asClientRegistrations();
		ClientRegistration adapted = registrations.get("registration");
		assertThat(adapted).isNotNull();
		ProviderDetails adaptedProvider = adapted.getProviderDetails();
		assertThat(adaptedProvider.getAuthorizationUri()).isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(adaptedProvider.getTokenUri()).isEqualTo("https://www.googleapis.com/oauth2/v4/token");
		UserInfoEndpoint userInfoEndpoint = adaptedProvider.getUserInfoEndpoint();
		assertThat(userInfoEndpoint.getUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/userinfo");
		assertThat(userInfoEndpoint.getUserNameAttributeName()).isEqualTo(IdTokenClaimNames.SUB);
		assertThat(adaptedProvider.getJwkSetUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
		assertThat(adapted.getRegistrationId()).isEqualTo("registration");
		assertThat(adapted.getClientId()).isEqualTo("clientId");
		assertThat(adapted.getClientSecret()).isEqualTo("clientSecret");
		assertThat(adapted.getClientAuthenticationMethod())
			.isEqualTo(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
		assertThat(adapted.getAuthorizationGrantType())
			.isEqualTo(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRedirectUri()).isEqualTo("{baseUrl}/{action}/oauth2/code/{registrationId}");
		assertThat(adapted.getScopes()).containsExactly("openid", "profile", "email");
		assertThat(adapted.getClientName()).isEqualTo("Google");
	}

	@Test
	void getClientRegistrationsWhenUsingCommonProviderWithOverrideShouldAdapt() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		OAuth2ClientProperties.Registration registration = createRegistration("google");
		registration.setClientName("clientName");
		properties.getRegistration().put("registration", registration);
		Map<String, ClientRegistration> registrations = new OAuth2ClientPropertiesMapper(properties)
			.asClientRegistrations();
		ClientRegistration adapted = registrations.get("registration");
		assertThat(adapted).isNotNull();
		ProviderDetails adaptedProvider = adapted.getProviderDetails();
		assertThat(adaptedProvider.getAuthorizationUri()).isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(adaptedProvider.getTokenUri()).isEqualTo("https://www.googleapis.com/oauth2/v4/token");
		UserInfoEndpoint userInfoEndpoint = adaptedProvider.getUserInfoEndpoint();
		assertThat(userInfoEndpoint.getUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/userinfo");
		assertThat(userInfoEndpoint.getUserNameAttributeName()).isEqualTo(IdTokenClaimNames.SUB);
		assertThat(userInfoEndpoint.getAuthenticationMethod())
			.isEqualTo(org.springframework.security.oauth2.core.AuthenticationMethod.HEADER);
		assertThat(adaptedProvider.getJwkSetUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
		assertThat(adapted.getRegistrationId()).isEqualTo("registration");
		assertThat(adapted.getClientId()).isEqualTo("clientId");
		assertThat(adapted.getClientSecret()).isEqualTo("clientSecret");
		assertThat(adapted.getClientAuthenticationMethod())
			.isEqualTo(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_POST);
		assertThat(adapted.getAuthorizationGrantType())
			.isEqualTo(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRedirectUri()).isEqualTo("https://example.com/redirect");
		assertThat(adapted.getScopes()).containsExactly("user");
		assertThat(adapted.getClientName()).isEqualTo("clientName");
	}

	@Test
	void getClientRegistrationsWhenUnknownProviderShouldThrowException() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
		registration.setProvider("missing");
		properties.getRegistration().put("registration", registration);
		assertThatIllegalStateException()
			.isThrownBy(() -> new OAuth2ClientPropertiesMapper(properties).asClientRegistrations())
			.withMessageContaining("Unknown provider ID 'missing'");
	}

	@Test
	void getClientRegistrationsWhenProviderNotSpecifiedShouldUseRegistrationId() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
		registration.setClientId("clientId");
		registration.setClientSecret("clientSecret");
		properties.getRegistration().put("google", registration);
		Map<String, ClientRegistration> registrations = new OAuth2ClientPropertiesMapper(properties)
			.asClientRegistrations();
		ClientRegistration adapted = registrations.get("google");
		assertThat(adapted).isNotNull();
		ProviderDetails adaptedProvider = adapted.getProviderDetails();
		assertThat(adaptedProvider.getAuthorizationUri()).isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(adaptedProvider.getTokenUri()).isEqualTo("https://www.googleapis.com/oauth2/v4/token");
		UserInfoEndpoint userInfoEndpoint = adaptedProvider.getUserInfoEndpoint();
		assertThat(userInfoEndpoint.getUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/userinfo");
		assertThat(userInfoEndpoint.getAuthenticationMethod())
			.isEqualTo(org.springframework.security.oauth2.core.AuthenticationMethod.HEADER);
		assertThat(adaptedProvider.getJwkSetUri()).isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
		assertThat(adapted.getRegistrationId()).isEqualTo("google");
		assertThat(adapted.getClientId()).isEqualTo("clientId");
		assertThat(adapted.getClientSecret()).isEqualTo("clientSecret");
		assertThat(adapted.getClientAuthenticationMethod())
			.isEqualTo(org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
		assertThat(adapted.getAuthorizationGrantType())
			.isEqualTo(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRedirectUri()).isEqualTo("{baseUrl}/{action}/oauth2/code/{registrationId}");
		assertThat(adapted.getScopes()).containsExactly("openid", "profile", "email");
		assertThat(adapted.getClientName()).isEqualTo("Google");
	}

	@Test
	void getClientRegistrationsWhenProviderNotSpecifiedAndUnknownProviderShouldThrowException() {
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
		properties.getRegistration().put("missing", registration);
		assertThatIllegalStateException()
			.isThrownBy(() -> new OAuth2ClientPropertiesMapper(properties).asClientRegistrations())
			.withMessageContaining("Provider ID must be specified for client registration 'missing'");
	}

	@Test
	void oidcProviderConfigurationWhenProviderNotSpecifiedOnRegistration() throws Exception {
		Registration login = new OAuth2ClientProperties.Registration();
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		testIssuerConfiguration(login, "okta", 0, 1);
	}

	@Test
	void oidcProviderConfigurationWhenProviderSpecifiedOnRegistration() throws Exception {
		OAuth2ClientProperties.Registration login = new Registration();
		login.setProvider("okta-oidc");
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		testIssuerConfiguration(login, "okta-oidc", 0, 1);
	}

	@Test
	void issuerUriConfigurationTriesOidcRfc8414UriSecond() throws Exception {
		OAuth2ClientProperties.Registration login = new Registration();
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		testIssuerConfiguration(login, "okta", 1, 2);
	}

	@Test
	void issuerUriConfigurationTriesOAuthMetadataUriThird() throws Exception {
		OAuth2ClientProperties.Registration login = new Registration();
		login.setClientId("clientId");
		login.setClientSecret("clientSecret");
		testIssuerConfiguration(login, "okta", 2, 3);
	}

	@Test
	void oidcProviderConfigurationWithCustomConfigurationOverridesProviderDefaults() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		setupMockResponse(issuer);
		OAuth2ClientProperties.Registration registration = createRegistration("okta-oidc");
		Provider provider = createProvider();
		provider.setIssuerUri(issuer);
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		properties.getProvider().put("okta-oidc", provider);
		properties.getRegistration().put("okta", registration);
		Map<String, ClientRegistration> registrations = new OAuth2ClientPropertiesMapper(properties)
			.asClientRegistrations();
		ClientRegistration adapted = registrations.get("okta");
		assertThat(adapted).isNotNull();
		ProviderDetails providerDetails = adapted.getProviderDetails();
		assertThat(adapted.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_POST);
		assertThat(adapted.getAuthorizationGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRegistrationId()).isEqualTo("okta");
		assertThat(adapted.getClientName()).isEqualTo(issuer);
		assertThat(adapted.getScopes()).containsOnly("user");
		assertThat(adapted.getRedirectUri()).isEqualTo("https://example.com/redirect");
		assertThat(providerDetails.getAuthorizationUri()).isEqualTo("https://example.com/auth");
		assertThat(providerDetails.getTokenUri()).isEqualTo("https://example.com/token");
		assertThat(providerDetails.getJwkSetUri()).isEqualTo("https://example.com/jwk");
		UserInfoEndpoint userInfoEndpoint = providerDetails.getUserInfoEndpoint();
		assertThat(userInfoEndpoint.getUri()).isEqualTo("https://example.com/info");
		assertThat(userInfoEndpoint.getUserNameAttributeName()).isEqualTo("sub");
	}

	private Provider createProvider() {
		Provider provider = new Provider();
		provider.setAuthorizationUri("https://example.com/auth");
		provider.setTokenUri("https://example.com/token");
		provider.setUserInfoUri("https://example.com/info");
		provider.setUserNameAttribute("sub");
		provider.setJwkSetUri("https://example.com/jwk");
		return provider;
	}

	private OAuth2ClientProperties.Registration createRegistration(String provider) {
		OAuth2ClientProperties.Registration registration = new OAuth2ClientProperties.Registration();
		registration.setProvider(provider);
		registration.setClientId("clientId");
		registration.setClientSecret("clientSecret");
		registration.setClientAuthenticationMethod("client_secret_post");
		registration.setRedirectUri("https://example.com/redirect");
		registration.setScope(Collections.singleton("user"));
		registration.setAuthorizationGrantType("authorization_code");
		return registration;
	}

	private void testIssuerConfiguration(OAuth2ClientProperties.Registration registration, String providerId,
			int errorResponseCount, int numberOfRequests) throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		setupMockResponsesWithErrors(issuer, errorResponseCount);
		OAuth2ClientProperties properties = new OAuth2ClientProperties();
		Provider provider = new Provider();
		provider.setIssuerUri(issuer);
		properties.getProvider().put(providerId, provider);
		properties.getRegistration().put("okta", registration);
		Map<String, ClientRegistration> registrations = new OAuth2ClientPropertiesMapper(properties)
			.asClientRegistrations();
		ClientRegistration adapted = registrations.get("okta");
		assertThat(adapted).isNotNull();
		ProviderDetails providerDetails = adapted.getProviderDetails();
		assertThat(adapted.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
		assertThat(adapted.getAuthorizationGrantType()).isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(adapted.getRegistrationId()).isEqualTo("okta");
		assertThat(adapted.getClientName()).isEqualTo(issuer);
		assertThat(adapted.getScopes()).isNull();
		assertThat(providerDetails.getAuthorizationUri()).isEqualTo("https://example.com/o/oauth2/v2/auth");
		assertThat(providerDetails.getTokenUri()).isEqualTo("https://example.com/oauth2/v4/token");
		assertThat(providerDetails.getJwkSetUri()).isEqualTo("https://example.com/oauth2/v3/certs");
		UserInfoEndpoint userInfoEndpoint = providerDetails.getUserInfoEndpoint();
		assertThat(userInfoEndpoint.getUri()).isEqualTo("https://example.com/oauth2/v3/userinfo");
		assertThat(userInfoEndpoint.getAuthenticationMethod())
			.isEqualTo(org.springframework.security.oauth2.core.AuthenticationMethod.HEADER);
		assertThat(this.server.getRequestCount()).isEqualTo(numberOfRequests);
	}

	private void setupMockResponse(String issuer) {
		MockResponse mockResponse = new MockResponse().setResponseCode(HttpStatus.OK.value())
			.setBody(new JsonMapper().writeValueAsString(getResponse(issuer)))
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		assertThat(this.server).isNotNull();
		this.server.enqueue(mockResponse);
	}

	private void setupMockResponsesWithErrors(String issuer, int errorResponseCount) {
		assertThat(this.server).isNotNull();
		for (int i = 0; i < errorResponseCount; i++) {
			MockResponse emptyResponse = new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
			this.server.enqueue(emptyResponse);
		}
		setupMockResponse(issuer);
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
		response.put("grant_types_supported", Collections.singletonList("authorization_code"));
		response.put("token_endpoint", "https://example.com/oauth2/v4/token");
		response.put("token_endpoint_auth_methods_supported", Collections.singletonList("client_secret_basic"));
		response.put("userinfo_endpoint", "https://example.com/oauth2/v3/userinfo");
		return response;
	}

}
