/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * OAuth 2.0 resource server properties.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author Mushtaq Ahmed
 * @author Yan Kardziyaka
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver")
public class OAuth2ResourceServerProperties {

	private final Jwt jwt = new Jwt();

	/**
	 * Returns the Jwt object associated with this OAuth2ResourceServerProperties
	 * instance.
	 * @return the Jwt object
	 */
	public Jwt getJwt() {
		return this.jwt;
	}

	private final Opaquetoken opaqueToken = new Opaquetoken();

	/**
	 * Returns the opaque token associated with this OAuth2ResourceServerProperties
	 * instance.
	 * @return the opaque token
	 */
	public Opaquetoken getOpaquetoken() {
		return this.opaqueToken;
	}

	/**
	 * Jwt class.
	 */
	public static class Jwt {

		/**
		 * JSON Web Key URI to use to verify the JWT token.
		 */
		private String jwkSetUri;

		/**
		 * JSON Web Algorithms used for verifying the digital signatures.
		 */
		private List<String> jwsAlgorithms = Arrays.asList("RS256");

		/**
		 * URI that can either be an OpenID Connect discovery endpoint or an OAuth 2.0
		 * Authorization Server Metadata endpoint defined by RFC 8414.
		 */
		private String issuerUri;

		/**
		 * Location of the file containing the public key used to verify a JWT.
		 */
		private Resource publicKeyLocation;

		/**
		 * Identifies the recipients that the JWT is intended for.
		 */
		private List<String> audiences = new ArrayList<>();

		/**
		 * Prefix to use for {@link GrantedAuthority authorities} mapped from JWT.
		 */
		private String authorityPrefix;

		/**
		 * Regex to use for splitting the value of the authorities claim into
		 * {@link GrantedAuthority authorities}.
		 */
		private String authoritiesClaimDelimiter;

		/**
		 * Name of token claim to use for mapping {@link GrantedAuthority authorities}
		 * from JWT.
		 */
		private String authoritiesClaimName;

		/**
		 * JWT principal claim name.
		 */
		private String principalClaimName;

		/**
		 * Returns the URI of the JSON Web Key (JWK) Set.
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
		 * Returns the list of supported JWS (JSON Web Signature) algorithms.
		 * @return the list of supported JWS algorithms
		 */
		public List<String> getJwsAlgorithms() {
			return this.jwsAlgorithms;
		}

		/**
		 * Sets the supported JWS algorithms for the JWT.
		 * @param jwsAlgorithms the list of JWS algorithms to be supported
		 */
		public void setJwsAlgorithms(List<String> jwsAlgorithms) {
			this.jwsAlgorithms = jwsAlgorithms;
		}

		/**
		 * Returns the issuer URI of the JWT.
		 * @return the issuer URI of the JWT
		 */
		public String getIssuerUri() {
			return this.issuerUri;
		}

		/**
		 * Sets the issuer URI for the JWT.
		 * @param issuerUri the issuer URI to be set
		 */
		public void setIssuerUri(String issuerUri) {
			this.issuerUri = issuerUri;
		}

		/**
		 * Returns the location of the public key.
		 * @return the location of the public key
		 */
		public Resource getPublicKeyLocation() {
			return this.publicKeyLocation;
		}

		/**
		 * Sets the location of the public key used for JWT verification.
		 * @param publicKeyLocation the location of the public key
		 */
		public void setPublicKeyLocation(Resource publicKeyLocation) {
			this.publicKeyLocation = publicKeyLocation;
		}

		/**
		 * Returns the list of audiences for the JWT.
		 * @return the list of audiences
		 */
		public List<String> getAudiences() {
			return this.audiences;
		}

		/**
		 * Sets the audiences for the JWT.
		 * @param audiences the list of audiences to be set
		 */
		public void setAudiences(List<String> audiences) {
			this.audiences = audiences;
		}

		/**
		 * Returns the authority prefix used in the JWT token.
		 * @return the authority prefix
		 */
		public String getAuthorityPrefix() {
			return this.authorityPrefix;
		}

		/**
		 * Sets the authority prefix for the JWT.
		 * @param authorityPrefix the authority prefix to be set
		 */
		public void setAuthorityPrefix(String authorityPrefix) {
			this.authorityPrefix = authorityPrefix;
		}

		/**
		 * Returns the delimiter used to separate authorities in the authorities claim of
		 * a JWT.
		 * @return the authorities claim delimiter
		 */
		public String getAuthoritiesClaimDelimiter() {
			return this.authoritiesClaimDelimiter;
		}

		/**
		 * Sets the delimiter used to separate authorities in the authorities claim of a
		 * JWT.
		 * @param authoritiesClaimDelimiter the delimiter to be used
		 */
		public void setAuthoritiesClaimDelimiter(String authoritiesClaimDelimiter) {
			this.authoritiesClaimDelimiter = authoritiesClaimDelimiter;
		}

		/**
		 * Returns the name of the authorities claim.
		 * @return the name of the authorities claim
		 */
		public String getAuthoritiesClaimName() {
			return this.authoritiesClaimName;
		}

		/**
		 * Sets the name of the authorities claim in the JWT.
		 * @param authoritiesClaimName the name of the authorities claim
		 */
		public void setAuthoritiesClaimName(String authoritiesClaimName) {
			this.authoritiesClaimName = authoritiesClaimName;
		}

		/**
		 * Returns the name of the principal claim.
		 * @return the name of the principal claim
		 */
		public String getPrincipalClaimName() {
			return this.principalClaimName;
		}

		/**
		 * Sets the name of the principal claim in the JSON Web Token (JWT).
		 * @param principalClaimName the name of the principal claim
		 */
		public void setPrincipalClaimName(String principalClaimName) {
			this.principalClaimName = principalClaimName;
		}

		/**
		 * Reads the public key from the specified location.
		 * @return the public key as a string
		 * @throws IOException if an I/O error occurs while reading the public key
		 * @throws InvalidConfigurationPropertyValueException if the public key location
		 * does not exist
		 */
		public String readPublicKey() throws IOException {
			String key = "spring.security.oauth2.resourceserver.public-key-location";
			Assert.notNull(this.publicKeyLocation, "PublicKeyLocation must not be null");
			if (!this.publicKeyLocation.exists()) {
				throw new InvalidConfigurationPropertyValueException(key, this.publicKeyLocation,
						"Public key location does not exist");
			}
			try (InputStream inputStream = this.publicKeyLocation.getInputStream()) {
				return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
			}
		}

	}

	/**
	 * Opaquetoken class.
	 */
	public static class Opaquetoken {

		/**
		 * Client id used to authenticate with the token introspection endpoint.
		 */
		private String clientId;

		/**
		 * Client secret used to authenticate with the token introspection endpoint.
		 */
		private String clientSecret;

		/**
		 * OAuth 2.0 endpoint through which token introspection is accomplished.
		 */
		private String introspectionUri;

		/**
		 * Returns the client ID associated with this Opaquetoken.
		 * @return the client ID
		 */
		public String getClientId() {
			return this.clientId;
		}

		/**
		 * Sets the client ID for the Opaquetoken.
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
		 * Sets the client secret for the Opaquetoken.
		 * @param clientSecret the client secret to be set
		 */
		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		/**
		 * Returns the URI for introspection.
		 * @return the URI for introspection
		 */
		public String getIntrospectionUri() {
			return this.introspectionUri;
		}

		/**
		 * Sets the URI for introspection.
		 * @param introspectionUri the URI for introspection
		 */
		public void setIntrospectionUri(String introspectionUri) {
			this.introspectionUri = introspectionUri;
		}

	}

}
