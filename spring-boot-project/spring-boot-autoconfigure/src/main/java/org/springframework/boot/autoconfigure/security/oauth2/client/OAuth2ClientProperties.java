/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.client")
public class OAuth2ClientProperties implements InitializingBean {

	/**
	 * OAuth provider details.
	 */
	private final Map<String, Provider> provider = new HashMap<>();

	/**
	 * OAuth client registrations.
	 */
	private final Map<String, Registration> registration = new HashMap<>();

	/**
	 * Returns the map of providers.
	 * @return the map of providers
	 */
	public Map<String, Provider> getProvider() {
		return this.provider;
	}

	/**
	 * Returns the registration map containing the OAuth2 client registrations.
	 * @return the registration map containing the OAuth2 client registrations
	 */
	public Map<String, Registration> getRegistration() {
		return this.registration;
	}

	/**
	 * This method is called after all bean properties have been set, and it is used to
	 * perform any necessary initialization tasks. It validates the properties of the
	 * OAuth2ClientProperties class.
	 */
	@Override
	public void afterPropertiesSet() {
		validate();
	}

	/**
	 * Validates the registration values in the OAuth2ClientProperties.
	 *
	 * This method iterates over the registration values in the OAuth2ClientProperties and
	 * calls the validateRegistration method for each registration.
	 *
	 * @see OAuth2ClientProperties#getRegistration()
	 * @see OAuth2ClientProperties#validateRegistration(OAuth2ClientRegistration)
	 */
	public void validate() {
		getRegistration().values().forEach(this::validateRegistration);
	}

	/**
	 * Validates the registration by checking if the client id is empty.
	 * @param registration the registration to be validated
	 * @throws IllegalStateException if the client id is empty
	 */
	private void validateRegistration(Registration registration) {
		if (!StringUtils.hasText(registration.getClientId())) {
			throw new IllegalStateException("Client id must not be empty.");
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
		private String provider;

		/**
		 * Client ID for the registration.
		 */
		private String clientId;

		/**
		 * Client secret of the registration.
		 */
		private String clientSecret;

		/**
		 * Client authentication method. May be left blank when using a pre-defined
		 * provider.
		 */
		private String clientAuthenticationMethod;

		/**
		 * Authorization grant type. May be left blank when using a pre-defined provider.
		 */
		private String authorizationGrantType;

		/**
		 * Redirect URI. May be left blank when using a pre-defined provider.
		 */
		private String redirectUri;

		/**
		 * Authorization scopes. When left blank the provider's default scopes, if any,
		 * will be used.
		 */
		private Set<String> scope;

		/**
		 * Client name. May be left blank when using a pre-defined provider.
		 */
		private String clientName;

		/**
		 * Returns the provider of the registration.
		 * @return the provider of the registration
		 */
		public String getProvider() {
			return this.provider;
		}

		/**
		 * Sets the provider for the registration.
		 * @param provider the provider to be set
		 */
		public void setProvider(String provider) {
			this.provider = provider;
		}

		/**
		 * Returns the client ID associated with the registration.
		 * @return the client ID
		 */
		public String getClientId() {
			return this.clientId;
		}

		/**
		 * Sets the client ID for the registration.
		 * @param clientId the client ID to be set
		 */
		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		/**
		 * Returns the client secret.
		 * @return the client secret
		 */
		public String getClientSecret() {
			return this.clientSecret;
		}

		/**
		 * Sets the client secret for the registration.
		 * @param clientSecret the client secret to be set
		 */
		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		/**
		 * Returns the client authentication method used by the registration.
		 * @return the client authentication method
		 */
		public String getClientAuthenticationMethod() {
			return this.clientAuthenticationMethod;
		}

		/**
		 * Sets the client authentication method for the registration.
		 * @param clientAuthenticationMethod the client authentication method to be set
		 */
		public void setClientAuthenticationMethod(String clientAuthenticationMethod) {
			this.clientAuthenticationMethod = clientAuthenticationMethod;
		}

		/**
		 * Returns the authorization grant type.
		 * @return the authorization grant type
		 */
		public String getAuthorizationGrantType() {
			return this.authorizationGrantType;
		}

		/**
		 * Sets the authorization grant type for the registration.
		 * @param authorizationGrantType the authorization grant type to be set
		 */
		public void setAuthorizationGrantType(String authorizationGrantType) {
			this.authorizationGrantType = authorizationGrantType;
		}

		/**
		 * Returns the redirect URI of the registration.
		 * @return the redirect URI of the registration
		 */
		public String getRedirectUri() {
			return this.redirectUri;
		}

		/**
		 * Sets the redirect URI for the registration.
		 * @param redirectUri the redirect URI to be set
		 */
		public void setRedirectUri(String redirectUri) {
			this.redirectUri = redirectUri;
		}

		/**
		 * Returns the scope of the registration.
		 * @return the scope of the registration as a Set of Strings
		 */
		public Set<String> getScope() {
			return this.scope;
		}

		/**
		 * Sets the scope of the registration.
		 * @param scope the set of strings representing the scope
		 */
		public void setScope(Set<String> scope) {
			this.scope = scope;
		}

		/**
		 * Returns the name of the client.
		 * @return the name of the client
		 */
		public String getClientName() {
			return this.clientName;
		}

		/**
		 * Sets the name of the client.
		 * @param clientName the name of the client
		 */
		public void setClientName(String clientName) {
			this.clientName = clientName;
		}

	}

	/**
	 * Provider class.
	 */
	public static class Provider {

		/**
		 * Authorization URI for the provider.
		 */
		private String authorizationUri;

		/**
		 * Token URI for the provider.
		 */
		private String tokenUri;

		/**
		 * User info URI for the provider.
		 */
		private String userInfoUri;

		/**
		 * User info authentication method for the provider.
		 */
		private String userInfoAuthenticationMethod;

		/**
		 * Name of the attribute that will be used to extract the username from the call
		 * to 'userInfoUri'.
		 */
		private String userNameAttribute;

		/**
		 * JWK set URI for the provider.
		 */
		private String jwkSetUri;

		/**
		 * URI that can either be an OpenID Connect discovery endpoint or an OAuth 2.0
		 * Authorization Server Metadata endpoint defined by RFC 8414.
		 */
		private String issuerUri;

		/**
		 * Returns the authorization URI.
		 * @return the authorization URI
		 */
		public String getAuthorizationUri() {
			return this.authorizationUri;
		}

		/**
		 * Sets the authorization URI for the Provider.
		 * @param authorizationUri the authorization URI to be set
		 */
		public void setAuthorizationUri(String authorizationUri) {
			this.authorizationUri = authorizationUri;
		}

		/**
		 * Returns the token URI of the Provider.
		 * @return the token URI of the Provider
		 */
		public String getTokenUri() {
			return this.tokenUri;
		}

		/**
		 * Sets the token URI for the Provider.
		 * @param tokenUri the token URI to be set
		 */
		public void setTokenUri(String tokenUri) {
			this.tokenUri = tokenUri;
		}

		/**
		 * Returns the URI for retrieving user information.
		 * @return the URI for retrieving user information
		 */
		public String getUserInfoUri() {
			return this.userInfoUri;
		}

		/**
		 * Sets the URI for retrieving user information.
		 * @param userInfoUri the URI for retrieving user information
		 */
		public void setUserInfoUri(String userInfoUri) {
			this.userInfoUri = userInfoUri;
		}

		/**
		 * Returns the authentication method used for user information.
		 * @return the authentication method used for user information
		 */
		public String getUserInfoAuthenticationMethod() {
			return this.userInfoAuthenticationMethod;
		}

		/**
		 * Sets the authentication method for user information.
		 * @param userInfoAuthenticationMethod the authentication method to be set
		 */
		public void setUserInfoAuthenticationMethod(String userInfoAuthenticationMethod) {
			this.userInfoAuthenticationMethod = userInfoAuthenticationMethod;
		}

		/**
		 * Returns the value of the userNameAttribute property.
		 * @return the value of the userNameAttribute property
		 */
		public String getUserNameAttribute() {
			return this.userNameAttribute;
		}

		/**
		 * Sets the user name attribute for the Provider.
		 * @param userNameAttribute the user name attribute to be set
		 */
		public void setUserNameAttribute(String userNameAttribute) {
			this.userNameAttribute = userNameAttribute;
		}

		/**
		 * Returns the URI of the JWK Set.
		 * @return the URI of the JWK Set
		 */
		public String getJwkSetUri() {
			return this.jwkSetUri;
		}

		/**
		 * Sets the URI of the JSON Web Key (JWK) Set.
		 * @param jwkSetUri the URI of the JWK Set
		 */
		public void setJwkSetUri(String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		/**
		 * Returns the issuer URI of the Provider.
		 * @return the issuer URI of the Provider
		 */
		public String getIssuerUri() {
			return this.issuerUri;
		}

		/**
		 * Sets the issuer URI for the provider.
		 * @param issuerUri the issuer URI to be set
		 */
		public void setIssuerUri(String issuerUri) {
			this.issuerUri = issuerUri;
		}

	}

}
