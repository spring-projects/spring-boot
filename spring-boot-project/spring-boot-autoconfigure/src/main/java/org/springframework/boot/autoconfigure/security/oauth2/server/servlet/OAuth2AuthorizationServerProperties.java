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

	public String getIssuer() {
		return this.issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public Map<String, Client> getClient() {
		return this.client;
	}

	public Endpoint getEndpoint() {
		return this.endpoint;
	}

	@Override
	public void afterPropertiesSet() {
		validate();
	}

	public void validate() {
		getClient().values().forEach(this::validateClient);
	}

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

		public String getAuthorizationUri() {
			return this.authorizationUri;
		}

		public void setAuthorizationUri(String authorizationUri) {
			this.authorizationUri = authorizationUri;
		}

		public String getDeviceAuthorizationUri() {
			return this.deviceAuthorizationUri;
		}

		public void setDeviceAuthorizationUri(String deviceAuthorizationUri) {
			this.deviceAuthorizationUri = deviceAuthorizationUri;
		}

		public String getDeviceVerificationUri() {
			return this.deviceVerificationUri;
		}

		public void setDeviceVerificationUri(String deviceVerificationUri) {
			this.deviceVerificationUri = deviceVerificationUri;
		}

		public String getTokenUri() {
			return this.tokenUri;
		}

		public void setTokenUri(String tokenUri) {
			this.tokenUri = tokenUri;
		}

		public String getJwkSetUri() {
			return this.jwkSetUri;
		}

		public void setJwkSetUri(String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		public String getTokenRevocationUri() {
			return this.tokenRevocationUri;
		}

		public void setTokenRevocationUri(String tokenRevocationUri) {
			this.tokenRevocationUri = tokenRevocationUri;
		}

		public String getTokenIntrospectionUri() {
			return this.tokenIntrospectionUri;
		}

		public void setTokenIntrospectionUri(String tokenIntrospectionUri) {
			this.tokenIntrospectionUri = tokenIntrospectionUri;
		}

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

		public String getLogoutUri() {
			return this.logoutUri;
		}

		public void setLogoutUri(String logoutUri) {
			this.logoutUri = logoutUri;
		}

		public String getClientRegistrationUri() {
			return this.clientRegistrationUri;
		}

		public void setClientRegistrationUri(String clientRegistrationUri) {
			this.clientRegistrationUri = clientRegistrationUri;
		}

		public String getUserInfoUri() {
			return this.userInfoUri;
		}

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

		public Registration getRegistration() {
			return this.registration;
		}

		public boolean isRequireProofKey() {
			return this.requireProofKey;
		}

		public void setRequireProofKey(boolean requireProofKey) {
			this.requireProofKey = requireProofKey;
		}

		public boolean isRequireAuthorizationConsent() {
			return this.requireAuthorizationConsent;
		}

		public void setRequireAuthorizationConsent(boolean requireAuthorizationConsent) {
			this.requireAuthorizationConsent = requireAuthorizationConsent;
		}

		public String getJwkSetUri() {
			return this.jwkSetUri;
		}

		public void setJwkSetUri(String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		public String getTokenEndpointAuthenticationSigningAlgorithm() {
			return this.tokenEndpointAuthenticationSigningAlgorithm;
		}

		public void setTokenEndpointAuthenticationSigningAlgorithm(String tokenEndpointAuthenticationSigningAlgorithm) {
			this.tokenEndpointAuthenticationSigningAlgorithm = tokenEndpointAuthenticationSigningAlgorithm;
		}

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

		public String getClientId() {
			return this.clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return this.clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public String getClientName() {
			return this.clientName;
		}

		public void setClientName(String clientName) {
			this.clientName = clientName;
		}

		public Set<String> getClientAuthenticationMethods() {
			return this.clientAuthenticationMethods;
		}

		public void setClientAuthenticationMethods(Set<String> clientAuthenticationMethods) {
			this.clientAuthenticationMethods = clientAuthenticationMethods;
		}

		public Set<String> getAuthorizationGrantTypes() {
			return this.authorizationGrantTypes;
		}

		public void setAuthorizationGrantTypes(Set<String> authorizationGrantTypes) {
			this.authorizationGrantTypes = authorizationGrantTypes;
		}

		public Set<String> getRedirectUris() {
			return this.redirectUris;
		}

		public void setRedirectUris(Set<String> redirectUris) {
			this.redirectUris = redirectUris;
		}

		public Set<String> getPostLogoutRedirectUris() {
			return this.postLogoutRedirectUris;
		}

		public void setPostLogoutRedirectUris(Set<String> postLogoutRedirectUris) {
			this.postLogoutRedirectUris = postLogoutRedirectUris;
		}

		public Set<String> getScopes() {
			return this.scopes;
		}

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

		public Duration getAuthorizationCodeTimeToLive() {
			return this.authorizationCodeTimeToLive;
		}

		public void setAuthorizationCodeTimeToLive(Duration authorizationCodeTimeToLive) {
			this.authorizationCodeTimeToLive = authorizationCodeTimeToLive;
		}

		public Duration getAccessTokenTimeToLive() {
			return this.accessTokenTimeToLive;
		}

		public void setAccessTokenTimeToLive(Duration accessTokenTimeToLive) {
			this.accessTokenTimeToLive = accessTokenTimeToLive;
		}

		public String getAccessTokenFormat() {
			return this.accessTokenFormat;
		}

		public void setAccessTokenFormat(String accessTokenFormat) {
			this.accessTokenFormat = accessTokenFormat;
		}

		public Duration getDeviceCodeTimeToLive() {
			return this.deviceCodeTimeToLive;
		}

		public void setDeviceCodeTimeToLive(Duration deviceCodeTimeToLive) {
			this.deviceCodeTimeToLive = deviceCodeTimeToLive;
		}

		public boolean isReuseRefreshTokens() {
			return this.reuseRefreshTokens;
		}

		public void setReuseRefreshTokens(boolean reuseRefreshTokens) {
			this.reuseRefreshTokens = reuseRefreshTokens;
		}

		public Duration getRefreshTokenTimeToLive() {
			return this.refreshTokenTimeToLive;
		}

		public void setRefreshTokenTimeToLive(Duration refreshTokenTimeToLive) {
			this.refreshTokenTimeToLive = refreshTokenTimeToLive;
		}

		public String getIdTokenSignatureAlgorithm() {
			return this.idTokenSignatureAlgorithm;
		}

		public void setIdTokenSignatureAlgorithm(String idTokenSignatureAlgorithm) {
			this.idTokenSignatureAlgorithm = idTokenSignatureAlgorithm;
		}

	}

}
