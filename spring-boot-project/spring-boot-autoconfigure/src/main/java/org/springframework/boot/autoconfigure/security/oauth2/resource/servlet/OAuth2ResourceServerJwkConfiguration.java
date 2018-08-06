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
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;

/**
 * Configures a {@link JwtDecoder} when a JWK Set URI is available.
 *
 * @author Madhura Bhave
 */
@Configuration
class OAuth2ResourceServerJwkConfiguration {

	private final OAuth2ResourceServerProperties properties;

	public OAuth2ResourceServerJwkConfiguration(
			OAuth2ResourceServerProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnProperty(name = "spring.security.oauth2.resource.jwt.jwk.set-uri")
	@ConditionalOnMissingBean
	public JwtDecoder jwtDecoder() {
		return new NimbusJwtDecoderJwkSupport(
				this.properties.getJwt().getJwk().getSetUri());
	}

}
