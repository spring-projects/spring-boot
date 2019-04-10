/*
 * Copyright 2012-2019 the original author or authors.
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
 * @since 2.1.0
 */
@ConfigurationProperties(prefix = "spring.security.oauth2.resourceserver")
public class OAuth2ResourceServerProperties {

	private final Jwt jwt = new Jwt();

	public Jwt getJwt() {
		return this.jwt;
	}

	public static class Jwt {

		/**
		 * JSON Web Key URI to use to verify the JWT token.
		 */
		private String jwkSetUri;

		/**
		 * JSON Web Algorithm used for verifying the digital signatures.
		 */
		private String jwsAlgorithm = "RS256";

		/**
		 * URI that an OpenID Connect Provider asserts as its Issuer Identifier.
		 */
		private String issuerUri;

		/**
		 * Location of the file containing the public key used to verify a JWT.
		 */
		private Resource publicKeyLocation;

		public String getJwkSetUri() {
			return this.jwkSetUri;
		}

		public void setJwkSetUri(String jwkSetUri) {
			this.jwkSetUri = jwkSetUri;
		}

		public String getJwsAlgorithm() {
			return this.jwsAlgorithm;
		}

		public void setJwsAlgorithm(String jwsAlgorithm) {
			this.jwsAlgorithm = jwsAlgorithm;
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

		public String readPublicKey() throws IOException {
			String key = "spring.security.oauth2.resourceserver.public-key-location";
			Assert.notNull(this.publicKeyLocation, "PublicKeyLocation must not be null");
			if (!this.publicKeyLocation.exists()) {
				throw new InvalidConfigurationPropertyValueException(key,
						this.publicKeyLocation, "Public key location does not exist");
			}
			try (InputStream inputStream = this.publicKeyLocation.getInputStream()) {
				return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
			}
		}

	}

}
