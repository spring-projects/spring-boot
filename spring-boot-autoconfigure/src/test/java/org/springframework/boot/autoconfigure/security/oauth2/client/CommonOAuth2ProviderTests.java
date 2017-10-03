/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.registration.ClientRegistration.ProviderDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CommonOAuth2Provider}.
 *
 * @author Phillip Webb
 */
public class CommonOAuth2ProviderTests {

	private static final String DEFAULT_REDIRECT_URL = "{scheme}://{serverName}:{serverPort}{contextPath}/oauth2/authorize/code/{registrationId}";

	@Test
	public void getBuilderWhenGoogleShouldHaveGoogleSettings() throws Exception {
		ClientRegistration registration = build(CommonOAuth2Provider.GOOGLE);
		ProviderDetails providerDetails = registration.getProviderDetails();
		assertThat(providerDetails.getAuthorizationUri())
				.isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(providerDetails.getTokenUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v4/token");
		assertThat(providerDetails.getUserInfoEndpoint().getUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/userinfo");
		assertThat(providerDetails.getUserInfoEndpoint().getUserNameAttributeName())
				.isEqualTo(null);
		assertThat(providerDetails.getJwkSetUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
		assertThat(registration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.BASIC);
		assertThat(registration.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(registration.getRedirectUri()).isEqualTo(DEFAULT_REDIRECT_URL);
		assertThat(registration.getScope()).containsOnly("openid", "profile", "email",
				"address", "phone");
		assertThat(registration.getClientName()).isEqualTo("Google");
		assertThat(registration.getRegistrationId()).isEqualTo("123");
	}

	@Test
	public void getBuilderWhenGitHubShouldHaveGitHubSettings() throws Exception {
		ClientRegistration registration = build(CommonOAuth2Provider.GITHUB);
		ProviderDetails providerDetails = registration.getProviderDetails();
		assertThat(providerDetails.getAuthorizationUri())
				.isEqualTo("https://github.com/login/oauth/authorize");
		assertThat(providerDetails.getTokenUri())
				.isEqualTo("https://github.com/login/oauth/access_token");
		assertThat(providerDetails.getUserInfoEndpoint().getUri())
				.isEqualTo("https://api.github.com/user");
		assertThat(providerDetails.getUserInfoEndpoint().getUserNameAttributeName())
				.isEqualTo("name");
		assertThat(providerDetails.getJwkSetUri()).isNull();
		assertThat(registration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.BASIC);
		assertThat(registration.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(registration.getRedirectUri()).isEqualTo(DEFAULT_REDIRECT_URL);
		assertThat(registration.getScope()).containsOnly("user");
		assertThat(registration.getClientName()).isEqualTo("GitHub");
		assertThat(registration.getRegistrationId()).isEqualTo("123");
	}

	@Test
	public void getBuilderWhenFacebookShouldHaveFacebookSettings() throws Exception {
		ClientRegistration registration = build(CommonOAuth2Provider.FACEBOOK);
		ProviderDetails providerDetails = registration.getProviderDetails();
		assertThat(providerDetails.getAuthorizationUri())
				.isEqualTo("https://www.facebook.com/v2.8/dialog/oauth");
		assertThat(providerDetails.getTokenUri())
				.isEqualTo("https://graph.facebook.com/v2.8/oauth/access_token");
		assertThat(providerDetails.getUserInfoEndpoint().getUri())
				.isEqualTo("https://graph.facebook.com/me");
		assertThat(providerDetails.getUserInfoEndpoint().getUserNameAttributeName())
				.isEqualTo("name");
		assertThat(providerDetails.getJwkSetUri()).isNull();
		assertThat(registration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.POST);
		assertThat(registration.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(registration.getRedirectUri()).isEqualTo(DEFAULT_REDIRECT_URL);
		assertThat(registration.getScope()).containsOnly("public_profile", "email");
		assertThat(registration.getClientName()).isEqualTo("Facebook");
		assertThat(registration.getRegistrationId()).isEqualTo("123");
	}

	@Test
	public void getBuilderWhenOktaShouldHaveOktaSettings() throws Exception {
		ClientRegistration registration = builder(CommonOAuth2Provider.OKTA)
				.authorizationUri("http://example.com/auth")
				.tokenUri("http://example.com/token")
				.userInfoUri("http://example.com/info").build();
		ProviderDetails providerDetails = registration.getProviderDetails();
		assertThat(providerDetails.getAuthorizationUri())
				.isEqualTo("http://example.com/auth");
		assertThat(providerDetails.getTokenUri()).isEqualTo("http://example.com/token");
		assertThat(providerDetails.getUserInfoEndpoint().getUri()).isEqualTo("http://example.com/info");
		assertThat(providerDetails.getUserInfoEndpoint().getUserNameAttributeName())
				.isEqualTo(null);
		assertThat(providerDetails.getJwkSetUri()).isNull();
		assertThat(registration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.BASIC);
		assertThat(registration.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(registration.getRedirectUri()).isEqualTo(DEFAULT_REDIRECT_URL);
		assertThat(registration.getScope()).containsOnly("openid", "profile", "email",
				"address", "phone");
		assertThat(registration.getClientName()).isEqualTo("Okta");
		assertThat(registration.getRegistrationId()).isEqualTo("123");
	}

	private ClientRegistration build(CommonOAuth2Provider provider) {
		return builder(provider).build();
	}

	private Builder builder(CommonOAuth2Provider provider) {
		return provider.getBuilder("123")
				.clientId("abcd")
				.clientSecret("secret");
	}

}
