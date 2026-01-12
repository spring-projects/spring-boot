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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * OAuth 2.0 client properties.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author MyeongHyeon Lee
 * @author Moritz Halbritter
 * @since 4.0.0
 */
@ConfigurationProperties("spring.security.oauth2.client")
public class OAuth2ClientProperties implements InitializingBean {

	/**
	 * OAuth provider details.
	 */
	private final Map<String, Provider> provider = new HashMap<>();

	/**
	 * OAuth client registrations.
	 */
	private final Map<String, Registration> registration = new HashMap<>();

	public Map<String, Provider> getProvider() {
		return this.provider;
	}

	public Map<String, Registration> getRegistration() {
		return this.registration;
	}

	@Override
	public void afterPropertiesSet() {
		validate();
	}

	public void validate() {
		getRegistration().forEach(this::validateRegistration);
	}

	private void validateRegistration(String id, Registration registration) {
		if (!StringUtils.hasText(registration.getClientId())) {
			throw new IllegalStateException("Client id of registration '%s' must not be empty.".formatted(id));
		}
	}

	/**
	 * A single client registration.
	 */
	public static class Registration {

		/**
		 * Reference to the OAuth 2.0 provider to use. May reference an element from the
		 * 'provider' property or used one of the commonly used providers (google, github,
		 * facebook, okta).
		 */
		private @Nullable String provider;

		/**
		 * Client ID for the registration.
		 */
		private @Nullable String clientId;

		/**
		 * Client secret of the registration.
		 */
		private @Nullable String clientSecret;

		/**
		 * Client authentication method. May be left blank when using a pre-defined
		 * provider.
		 */
		private @Nullable String clientAuthenticationMethod;

		/**
		 * Authorization grant type. May be left blank when using a pre-defined provider.
		 */
		private @Nullable String authorizationGrantType;

		/**
		 * Redirect URI. May be left blank when using a pre-defined provider.
		 */
		private @Nullable String redirectUri;

		/**
		 * Authorization scopes. When left blank the provider's default scopes, if any,
		 * will be used.
		 */
		private @Nullable Set<String> scope;

		/**
		 * Client name. May be left blank when using a pre-defined provider.
		 */
		private @Nullable String clientName;

		public @Nullable String getProvider() {
			return this.provider;
		}

		public void setProvider(@Nullable String provider) {
			this.provider = provider;
		}

		public @Nullable String getClientId() {
			return this.clientId;
		}

		public void setClientId(@Nullable String clientId) {
			this.clientId = clientId;
		}

		public @Nullable String getClientSecret() {
			return this.clientSecret;
		}

		public void setClientSecret(@Nullable String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public @Nullable String getClientAuthenticationMethod() {
			return this.clientAuthenticationMethod;
		}

		public void setClientAuthenticationMethod(@Nullable String clientAuthenticationMethod) {
			this.clientAuthenticationMethod = clientAuthenticationMethod;
		}

		public @Nullable String getAuthorizationGrantType() {
			return this.authorizationGrantType;
		}

		public void setAuthorizationGrantType(@Nullable String authorizationGrantType) {
			this.authorizationGrantType = authorizationGrantType;
		}

		public @Nullable String getRedirectUri() {
			return this.redirectUri;
		}

		public void setRedirectUri(@Nullable String redirectUri) {
			this.redirectUri = redirectUri;
		}

		public @Nullable Set<String> getScope() {
			return this.scope;
		}

		public void setScope(@Nullable Set<String> scope) {
			this.scope = scope;
		}

		public @Nullable String getClientName() {
			return this.clientName;
		}

		public void setClientName(@Nullable String clientName) {
			this.clientName = clientName;
		}

	}

	public static class Provider {

		/**
		 * Authorization URI for the provider.
		 */
		private @Nullable String authorizationUri;

		/**
		 * Token URI for the provider.
		 */
		private @Nullable String tokenUri;

		/**
		 * User info URI for the provider.
		 */
		private @Nullable String userInfoUri;

		/**
		 * User info authentication method for the provider.
		 */
		private @Nullable String userInfoAuthenticationMethod;

		/**
		 * Name of the attribute that will be used to extract the username from the call
		 * to 'userInfoUri'.
		 */
		private @Nullable String userNameAttribute;

		/**
		 * JWK set URI for the provider.
		 */
		private @Nullable String jwkSetUri;

		/**
		 * URI that can either be an OpenID Connect discovery endpoint or an OAuth 2.0
		 * Authorization Server Metadata endpoint defined by RFC 8414.
		 */
		private @Nullable String issuerUri;

		public @Nullable String getAuthorizationUri() {
			return this.authorizationUri;
		}

		public void setAuthorizationUri(@Nullable String authorizationUri) {
			this.authorizationUri = authorizationUri;
		}

		public @Nullable String getTokenUri() {
			return this.tokenUri;
		}

		public void setTokenUri(@Nullable String tokenUri) {
			this.tokenUri = tokenUri;
		}

		public @Nullable String getUserInfoUri() {
			return this.userInfoUri;
		}

		public void setUserInfoUri(@Nullable String userInfoUri) {
			this.userInfoUri = userInfoUri;
		}

		public @Nullable String getUserInfoAuthenticationMethod() {
			return this.userInfoAuthenticationMethod;
		}

		public void setUserInfoAuthenticationMethod(@Nullable String userInfoAuthenticationMethod) {
			this.userInfoAuthenticationMethod = userInfoAuthenticationMethod;
		}

		public @Nullable String getUserNameAttribute() {
			return this.userNameAttribute;
		}

		public void setUserNameAttribute(@Nullable String userNameAttribute) {
			this.userNameAttribute = userNameAttribute;
		}

		public @Nullable String getJwkSetUri() {
			return this.jwkSetUri;
		}

		public void setJwkSetUri(@Nullable String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		public @Nullable String getIssuerUri() {
			return this.issuerUri;
		}

		public void setIssuerUri(@Nullable String issuerUri) {
			this.issuerUri = issuerUri;
		}

	}

}
