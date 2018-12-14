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
package org.springframework.boot.autoconfigure.security.oauth2.resource.servlet;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.IssuerUriCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;

/**
 * Configures a {@link JwtDecoder} when a JWK Set URI or OpenID Connect Issuer URI is
 * available.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 */
@Configuration
class OAuth2ResourceServerJwtConfiguration {

	private final OAuth2ResourceServerProperties.Jwt properties;

	OAuth2ResourceServerJwtConfiguration(OAuth2ResourceServerProperties properties) {
		this.properties = properties.getJwt();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
	@ConditionalOnMissingBean
	public JwtDecoder jwtDecoderByJwkKeySetUri() {
		return new NimbusJwtDecoderJwkSupport(this.properties.getJwkSetUri(),
				this.properties.getJwsAlgorithm());
	}

	@Bean
	@Conditional(IssuerUriCondition.class)
	@ConditionalOnMissingBean
	public JwtDecoder jwtDecoderByIssuerUri() {
		return JwtDecoders.fromOidcIssuerLocation(this.properties.getIssuerUri());
	}

}
