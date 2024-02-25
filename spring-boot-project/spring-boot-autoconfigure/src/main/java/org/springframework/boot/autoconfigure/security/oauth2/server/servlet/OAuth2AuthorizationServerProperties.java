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

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * OAuth 2.0 Authorization Server properties.
 *
 * @author Steve Riesenberg
 * @since 3.1.0
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.authorizationserver")
public class OAuth2AuthorizationServerProperties implements InitializingBean {

	/**
	 * URL of the Authorization Server's Issuer Identifier.
	 */
	private String issuer;

	/**
	 * Registered clients of the Authorization Server.
	 */
	private final Map<String, Client> client = new HashMap<>();

	/**
	 * Authorization Server endpoints.
	 */
	private final Endpoint endpoint = new Endpoint();

	/**
	 * Returns the issuer of the OAuth2 authorization server.
	 * @return the issuer of the OAuth2 authorization server
	 */
	public String getIssuer() {
		return this.issuer;
	}

	/**
	 * Sets the issuer for the OAuth2 authorization server.
	 * @param issuer the issuer to set
	 */
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	/**
	 * Returns the map of clients registered in the OAuth2 authorization server.
	 * @return the map of clients registered in the OAuth2 authorization server
	 */
	public Map<String, Client> getClient() {
		return this.client;
	}

	/**
	 * Returns the endpoint of the OAuth2 authorization server.
	 * @return the endpoint of the OAuth2 authorization server
	 */
	public Endpoint getEndpoint() {
		return this.endpoint;
	}

	/**
	 * This method is called after all bean properties have been set and performs
	 * validation on the properties of the OAuth2AuthorizationServerProperties class. It
	 * ensures that all required properties are properly set and throws an exception if
	 * any property is missing or invalid.
	 */
	@Override
	public void afterPropertiesSet() {
		validate();
	}

	/**
	 * Validates all clients in the OAuth2 authorization server.
	 *
	 * This method iterates over all clients in the authorization server and calls the
	 * {@link #validateClient(Client)} method to validate each client individually.
	 *
	 * @see #validateClient(Client)
	 */
	public void validate() {
		getClient().values().forEach(this::validateClient);
	}

	/**
	 * Validates the given client.
	 * @param client the client to validate
	 * @throws IllegalStateException if the client id is empty, client authentication
	 * methods are empty, or authorization grant types are empty
	 */
	private void validateClient(Client client) {
		if (!StringUtils.hasText(client.getRegistration().getClientId())) {
			throw new IllegalStateException("Client id must not be empty.");
		}
		if (CollectionUtils.isEmpty(client.getRegistration().getClientAuthenticationMethods())) {
			throw new IllegalStateException("Client authentication methods must not be empty.");
		}
		if (CollectionUtils.isEmpty(client.getRegistration().getAuthorizationGrantTypes())) {
			throw new IllegalStateException("Authorization grant types must not be empty.");
		}
	}

	/**
	 * Authorization Server endpoints.
	 */
	public static class Endpoint {

		/**
		 * Authorization Server's OAuth 2.0 Authorization Endpoint.
		 */
		private String authorizationUri = "/oauth2/authorize";

		/**
		 * Authorization Server's OAuth 2.0 Device Authorization Endpoint.
		 */
		private String deviceAuthorizationUri = "/oauth2/device_authorization";

		/**
		 * Authorization Server's OAuth 2.0 Device Verification Endpoint.
		 */
		private String deviceVerificationUri = "/oauth2/device_verification";

		/**
		 * Authorization Server's OAuth 2.0 Token Endpoint.
		 */
		private String tokenUri = "/oauth2/token";

		/**
		 * Authorization Server's JWK Set Endpoint.
		 */
		private String jwkSetUri = "/oauth2/jwks";

		/**
		 * Authorization Server's OAuth 2.0 Token Revocation Endpoint.
		 */
		private String tokenRevocationUri = "/oauth2/revoke";

		/**
		 * Authorization Server's OAuth 2.0 Token Introspection Endpoint.
		 */
		private String tokenIntrospectionUri = "/oauth2/introspect";

		/**
		 * OpenID Connect 1.0 endpoints.
		 */
		@NestedConfigurationProperty
		private final OidcEndpoint oidc = new OidcEndpoint();

		/**
		 * Returns the authorization URI.
		 * @return the authorization URI
		 */
		public String getAuthorizationUri() {
			return this.authorizationUri;
		}

		/**
		 * Sets the authorization URI for the endpoint.
		 * @param authorizationUri the authorization URI to be set
		 */
		public void setAuthorizationUri(String authorizationUri) {
			this.authorizationUri = authorizationUri;
		}

		/**
		 * Returns the device authorization URI.
		 * @return the device authorization URI
		 */
		public String getDeviceAuthorizationUri() {
			return this.deviceAuthorizationUri;
		}

		/**
		 * Sets the device authorization URI for this endpoint.
		 * @param deviceAuthorizationUri the device authorization URI to be set
		 */
		public void setDeviceAuthorizationUri(String deviceAuthorizationUri) {
			this.deviceAuthorizationUri = deviceAuthorizationUri;
		}

		/**
		 * Returns the device verification URI.
		 * @return the device verification URI
		 */
		public String getDeviceVerificationUri() {
			return this.deviceVerificationUri;
		}

		/**
		 * Sets the device verification URI.
		 * @param deviceVerificationUri the device verification URI to be set
		 */
		public void setDeviceVerificationUri(String deviceVerificationUri) {
			this.deviceVerificationUri = deviceVerificationUri;
		}

		/**
		 * Returns the token URI of the endpoint.
		 * @return the token URI of the endpoint
		 */
		public String getTokenUri() {
			return this.tokenUri;
		}

		/**
		 * Sets the token URI for the endpoint.
		 * @param tokenUri the token URI to be set
		 */
		public void setTokenUri(String tokenUri) {
			this.tokenUri = tokenUri;
		}

		/**
		 * Returns the URI of the JWK Set.
		 * @return the URI of the JWK Set
		 */
		public String getJwkSetUri() {
			return this.jwkSetUri;
		}

		/**
		 * Sets the URI of the JWK Set.
		 * @param jwkSetUri the URI of the JWK Set
		 */
		public void setJwkSetUri(String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		/**
		 * Returns the token revocation URI.
		 * @return the token revocation URI
		 */
		public String getTokenRevocationUri() {
			return this.tokenRevocationUri;
		}

		/**
		 * Sets the token revocation URI for the endpoint.
		 * @param tokenRevocationUri the token revocation URI to be set
		 */
		public void setTokenRevocationUri(String tokenRevocationUri) {
			this.tokenRevocationUri = tokenRevocationUri;
		}

		/**
		 * Returns the URI for token introspection.
		 * @return the URI for token introspection
		 */
		public String getTokenIntrospectionUri() {
			return this.tokenIntrospectionUri;
		}

		/**
		 * Sets the URI for token introspection.
		 * @param tokenIntrospectionUri the URI for token introspection
		 */
		public void setTokenIntrospectionUri(String tokenIntrospectionUri) {
			this.tokenIntrospectionUri = tokenIntrospectionUri;
		}

		/**
		 * Returns the OIDC endpoint.
		 * @return the OIDC endpoint
		 */
		public OidcEndpoint getOidc() {
			return this.oidc;
		}

	}

	/**
	 * OpenID Connect 1.0 endpoints.
	 */
	public static class OidcEndpoint {

		/**
		 * Authorization Server's OpenID Connect 1.0 Logout Endpoint.
		 */
		private String logoutUri = "/connect/logout";

		/**
		 * Authorization Server's OpenID Connect 1.0 Client Registration Endpoint.
		 */
		private String clientRegistrationUri = "/connect/register";

		/**
		 * Authorization Server's OpenID Connect 1.0 UserInfo Endpoint.
		 */
		private String userInfoUri = "/userinfo";

		/**
		 * Returns the logout URI for the OIDC endpoint.
		 * @return the logout URI
		 */
		public String getLogoutUri() {
			return this.logoutUri;
		}

		/**
		 * Sets the logout URI for the OIDC endpoint.
		 * @param logoutUri the logout URI to be set
		 */
		public void setLogoutUri(String logoutUri) {
			this.logoutUri = logoutUri;
		}

		/**
		 * Returns the client registration URI.
		 * @return the client registration URI
		 */
		public String getClientRegistrationUri() {
			return this.clientRegistrationUri;
		}

		/**
		 * Sets the client registration URI for the OIDC endpoint.
		 * @param clientRegistrationUri the client registration URI to be set
		 */
		public void setClientRegistrationUri(String clientRegistrationUri) {
			this.clientRegistrationUri = clientRegistrationUri;
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

	}

	/**
	 * A registered client of the Authorization Server.
	 */
	public static class Client {

		/**
		 * Client registration information.
		 */
		@NestedConfigurationProperty
		private final Registration registration = new Registration();

		/**
		 * Whether the client is required to provide a proof key challenge and verifier
		 * when performing the Authorization Code Grant flow.
		 */
		private boolean requireProofKey = false;

		/**
		 * Whether authorization consent is required when the client requests access.
		 */
		private boolean requireAuthorizationConsent = false;

		/**
		 * URL for the client's JSON Web Key Set.
		 */
		private String jwkSetUri;

		/**
		 * JWS algorithm that must be used for signing the JWT used to authenticate the
		 * client at the Token Endpoint for the {@code private_key_jwt} and
		 * {@code client_secret_jwt} authentication methods.
		 */
		private String tokenEndpointAuthenticationSigningAlgorithm;

		/**
		 * Token settings of the registered client.
		 */
		@NestedConfigurationProperty
		private final Token token = new Token();

		/**
		 * Returns the registration object associated with this client.
		 * @return the registration object
		 */
		public Registration getRegistration() {
			return this.registration;
		}

		/**
		 * Returns a boolean value indicating whether a proof key is required.
		 * @return true if a proof key is required, false otherwise
		 */
		public boolean isRequireProofKey() {
			return this.requireProofKey;
		}

		/**
		 * Sets whether a proof key is required for authentication.
		 * @param requireProofKey true if a proof key is required, false otherwise
		 */
		public void setRequireProofKey(boolean requireProofKey) {
			this.requireProofKey = requireProofKey;
		}

		/**
		 * Returns a boolean value indicating whether authorization consent is required.
		 * @return true if authorization consent is required, false otherwise
		 */
		public boolean isRequireAuthorizationConsent() {
			return this.requireAuthorizationConsent;
		}

		/**
		 * Sets whether authorization consent is required.
		 * @param requireAuthorizationConsent true if authorization consent is required,
		 * false otherwise
		 */
		public void setRequireAuthorizationConsent(boolean requireAuthorizationConsent) {
			this.requireAuthorizationConsent = requireAuthorizationConsent;
		}

		/**
		 * Returns the URI of the JWK Set.
		 * @return the URI of the JWK Set
		 */
		public String getJwkSetUri() {
			return this.jwkSetUri;
		}

		/**
		 * Sets the URI of the JWK Set.
		 * @param jwkSetUri the URI of the JWK Set
		 */
		public void setJwkSetUri(String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		/**
		 * Returns the token endpoint authentication signing algorithm used by the client.
		 * @return the token endpoint authentication signing algorithm
		 */
		public String getTokenEndpointAuthenticationSigningAlgorithm() {
			return this.tokenEndpointAuthenticationSigningAlgorithm;
		}

		/**
		 * Sets the token endpoint authentication signing algorithm.
		 * @param tokenEndpointAuthenticationSigningAlgorithm the signing algorithm to be
		 * used for token endpoint authentication
		 */
		public void setTokenEndpointAuthenticationSigningAlgorithm(String tokenEndpointAuthenticationSigningAlgorithm) {
			this.tokenEndpointAuthenticationSigningAlgorithm = tokenEndpointAuthenticationSigningAlgorithm;
		}

		/**
		 * Returns the token associated with this client.
		 * @return the token associated with this client
		 */
		public Token getToken() {
			return this.token;
		}

	}

	/**
	 * Client registration information.
	 */
	public static class Registration {

		/**
		 * Client ID of the registration.
		 */
		private String clientId;

		/**
		 * Client secret of the registration. May be left blank for a public client.
		 */
		private String clientSecret;

		/**
		 * Name of the client.
		 */
		private String clientName;

		/**
		 * Client authentication method(s) that the client may use.
		 */
		private Set<String> clientAuthenticationMethods = new HashSet<>();

		/**
		 * Authorization grant type(s) that the client may use.
		 */
		private Set<String> authorizationGrantTypes = new HashSet<>();

		/**
		 * Redirect URI(s) that the client may use in redirect-based flows.
		 */
		private Set<String> redirectUris = new HashSet<>();

		/**
		 * Redirect URI(s) that the client may use for logout.
		 */
		private Set<String> postLogoutRedirectUris = new HashSet<>();

		/**
		 * Scope(s) that the client may use.
		 */
		private Set<String> scopes = new HashSet<>();

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

		/**
		 * Returns the set of client authentication methods supported by the Registration.
		 * @return the set of client authentication methods
		 */
		public Set<String> getClientAuthenticationMethods() {
			return this.clientAuthenticationMethods;
		}

		/**
		 * Sets the client authentication methods for the registration.
		 * @param clientAuthenticationMethods the set of client authentication methods to
		 * be set
		 */
		public void setClientAuthenticationMethods(Set<String> clientAuthenticationMethods) {
			this.clientAuthenticationMethods = clientAuthenticationMethods;
		}

		/**
		 * Returns the set of authorization grant types supported by the registration.
		 * @return the set of authorization grant types
		 */
		public Set<String> getAuthorizationGrantTypes() {
			return this.authorizationGrantTypes;
		}

		/**
		 * Sets the authorization grant types for the registration.
		 * @param authorizationGrantTypes the set of authorization grant types to be set
		 */
		public void setAuthorizationGrantTypes(Set<String> authorizationGrantTypes) {
			this.authorizationGrantTypes = authorizationGrantTypes;
		}

		/**
		 * Returns the set of redirect URIs for the registration.
		 * @return the set of redirect URIs
		 */
		public Set<String> getRedirectUris() {
			return this.redirectUris;
		}

		/**
		 * Sets the redirect URIs for the registration.
		 * @param redirectUris the set of redirect URIs to be set
		 */
		public void setRedirectUris(Set<String> redirectUris) {
			this.redirectUris = redirectUris;
		}

		/**
		 * Returns the set of post-logout redirect URIs.
		 * @return the set of post-logout redirect URIs
		 */
		public Set<String> getPostLogoutRedirectUris() {
			return this.postLogoutRedirectUris;
		}

		/**
		 * Sets the post logout redirect URIs for the registration.
		 * @param postLogoutRedirectUris the set of post logout redirect URIs to be set
		 */
		public void setPostLogoutRedirectUris(Set<String> postLogoutRedirectUris) {
			this.postLogoutRedirectUris = postLogoutRedirectUris;
		}

		/**
		 * Returns the set of scopes associated with the registration.
		 * @return the set of scopes
		 */
		public Set<String> getScopes() {
			return this.scopes;
		}

		/**
		 * Sets the scopes for the registration.
		 * @param scopes the set of scopes to be set
		 */
		public void setScopes(Set<String> scopes) {
			this.scopes = scopes;
		}

	}

	/**
	 * Token settings of the registered client.
	 */
	public static class Token {

		/**
		 * Time-to-live for an authorization code.
		 */
		private Duration authorizationCodeTimeToLive = Duration.ofMinutes(5);

		/**
		 * Time-to-live for an access token.
		 */
		private Duration accessTokenTimeToLive = Duration.ofMinutes(5);

		/**
		 * Token format for an access token.
		 */
		private String accessTokenFormat = "self-contained";

		/**
		 * Time-to-live for a device code.
		 */
		private Duration deviceCodeTimeToLive = Duration.ofMinutes(5);

		/**
		 * Whether refresh tokens are reused or a new refresh token is issued when
		 * returning the access token response.
		 */
		private boolean reuseRefreshTokens = true;

		/**
		 * Time-to-live for a refresh token.
		 */
		private Duration refreshTokenTimeToLive = Duration.ofMinutes(60);

		/**
		 * JWS algorithm for signing the ID Token.
		 */
		private String idTokenSignatureAlgorithm = "RS256";

		/**
		 * Returns the time to live for an authorization code.
		 * @return the time to live for an authorization code
		 */
		public Duration getAuthorizationCodeTimeToLive() {
			return this.authorizationCodeTimeToLive;
		}

		/**
		 * Sets the time to live for the authorization code.
		 * @param authorizationCodeTimeToLive the duration for which the authorization
		 * code is valid
		 */
		public void setAuthorizationCodeTimeToLive(Duration authorizationCodeTimeToLive) {
			this.authorizationCodeTimeToLive = authorizationCodeTimeToLive;
		}

		/**
		 * Returns the time to live of the access token.
		 * @return the time to live of the access token
		 */
		public Duration getAccessTokenTimeToLive() {
			return this.accessTokenTimeToLive;
		}

		/**
		 * Sets the time to live for the access token.
		 * @param accessTokenTimeToLive the duration of time for which the access token is
		 * valid
		 */
		public void setAccessTokenTimeToLive(Duration accessTokenTimeToLive) {
			this.accessTokenTimeToLive = accessTokenTimeToLive;
		}

		/**
		 * Returns the format of the access token.
		 * @return the format of the access token
		 */
		public String getAccessTokenFormat() {
			return this.accessTokenFormat;
		}

		/**
		 * Sets the format of the access token.
		 * @param accessTokenFormat the format of the access token
		 */
		public void setAccessTokenFormat(String accessTokenFormat) {
			this.accessTokenFormat = accessTokenFormat;
		}

		/**
		 * Returns the time to live for the device code.
		 * @return the time to live for the device code
		 */
		public Duration getDeviceCodeTimeToLive() {
			return this.deviceCodeTimeToLive;
		}

		/**
		 * Sets the time to live for the device code.
		 * @param deviceCodeTimeToLive the duration of time for the device code to live
		 */
		public void setDeviceCodeTimeToLive(Duration deviceCodeTimeToLive) {
			this.deviceCodeTimeToLive = deviceCodeTimeToLive;
		}

		/**
		 * Returns a boolean value indicating whether refresh tokens can be reused.
		 * @return true if refresh tokens can be reused, false otherwise
		 */
		public boolean isReuseRefreshTokens() {
			return this.reuseRefreshTokens;
		}

		/**
		 * Sets whether to reuse refresh tokens.
		 * @param reuseRefreshTokens true to reuse refresh tokens, false otherwise
		 */
		public void setReuseRefreshTokens(boolean reuseRefreshTokens) {
			this.reuseRefreshTokens = reuseRefreshTokens;
		}

		/**
		 * Returns the time to live for the refresh token.
		 * @return the time to live for the refresh token
		 */
		public Duration getRefreshTokenTimeToLive() {
			return this.refreshTokenTimeToLive;
		}

		/**
		 * Sets the time to live for the refresh token.
		 * @param refreshTokenTimeToLive the duration of time for which the refresh token
		 * is valid
		 */
		public void setRefreshTokenTimeToLive(Duration refreshTokenTimeToLive) {
			this.refreshTokenTimeToLive = refreshTokenTimeToLive;
		}

		/**
		 * Returns the signature algorithm used for the ID token.
		 * @return the ID token signature algorithm
		 */
		public String getIdTokenSignatureAlgorithm() {
			return this.idTokenSignatureAlgorithm;
		}

		/**
		 * Sets the signature algorithm for the ID token.
		 * @param idTokenSignatureAlgorithm the signature algorithm to be set for the ID
		 * token
		 */
		public void setIdTokenSignatureAlgorithm(String idTokenSignatureAlgorithm) {
			this.idTokenSignatureAlgorithm = idTokenSignatureAlgorithm;
		}

	}

}
