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
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * OAuth 2.0 resource server properties.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author Mushtaq Ahmed
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver")
public class OAuth2ResourceServerProperties {

	private final Jwt jwt = new Jwt();

	public Jwt getJwt() {
		return this.jwt;
	}

	private final Opaquetoken opaqueToken = new Opaquetoken();

	public Opaquetoken getOpaquetoken() {
		return this.opaqueToken;
	}

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

		public String getJwkSetUri() {
			return this.jwkSetUri;
		}

		public void setJwkSetUri(String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		public List<String> getJwsAlgorithms() {
			return this.jwsAlgorithms;
		}

		public void setJwsAlgorithms(List<String> jwsAlgorithms) {
			this.jwsAlgorithms = jwsAlgorithms;
		}

		public String getIssuerUri() {
			return this.issuerUri;
		}

		public void setIssuerUri(String issuerUri) {
			this.issuerUri = issuerUri;
		}

		public Resource getPublicKeyLocation() {
			return this.publicKeyLocation;
		}

		public void setPublicKeyLocation(Resource publicKeyLocation) {
			this.publicKeyLocation = publicKeyLocation;
		}

		public List<String> getAudiences() {
			return this.audiences;
		}

		public void setAudiences(List<String> audiences) {
			this.audiences = audiences;
		}

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

		public String getIntrospectionUri() {
			return this.introspectionUri;
		}

		public void setIntrospectionUri(String introspectionUri) {
			this.introspectionUri = introspectionUri;
		}

	}

}
