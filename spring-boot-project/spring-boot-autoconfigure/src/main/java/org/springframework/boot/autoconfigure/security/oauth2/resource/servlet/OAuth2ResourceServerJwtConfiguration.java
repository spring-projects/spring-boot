/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource.servlet;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.autoconfigure.security.oauth2.resource.IssuerUriCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.KeyValueCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures a {@link JwtDecoder} when a JWK Set URI, OpenID Connect Issuer URI or Public
 * Key configuration is available. Also configures a {@link WebSecurityConfigurerAdapter}
 * if a {@link JwtDecoder} bean is found.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Mushtaq Ahmed
 */
@Configuration(proxyBeanMethods = false)
class OAuth2ResourceServerJwtConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(JwtDecoder.class)
	static class JwtDecoderConfiguration {

		private final OAuth2ResourceServerProperties.Jwt properties;

		JwtDecoderConfiguration(OAuth2ResourceServerProperties properties) {
			this.properties = properties.getJwt();
		}

		@Bean
		@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
		JwtDecoder jwtDecoderByJwkKeySetUri() {
			NimbusJwtDecoder nimbusJwtDecoder = NimbusJwtDecoder.withJwkSetUri(this.properties.getJwkSetUri())
					.jwsAlgorithm(SignatureAlgorithm.from(this.properties.getJwsAlgorithm())).build();
			List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
			validators.add(new JwtTimestampValidator());
			String issuerUri = this.properties.getIssuerUri();
			if (issuerUri != null) {
				validators.add(new JwtIssuerValidator(issuerUri));
			}
			String audience = this.properties.getAudience();
			if (audience != null) {
				validators.add(new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
						(aud) -> aud != null && aud.contains(audience)));
			}
			nimbusJwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
			return nimbusJwtDecoder;
		}

		@Bean
		@Conditional(KeyValueCondition.class)
		JwtDecoder jwtDecoderByPublicKeyValue() throws Exception {
			RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(getKeySpec(this.properties.readPublicKey())));
			return NimbusJwtDecoder.withPublicKey(publicKey)
					.signatureAlgorithm(SignatureAlgorithm.from(this.properties.getJwsAlgorithm())).build();
		}

		private byte[] getKeySpec(String keyValue) {
			keyValue = keyValue.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
			return Base64.getMimeDecoder().decode(keyValue);
		}

		@Bean
		@Conditional(IssuerUriCondition.class)
		SupplierJwtDecoder jwtDecoderByIssuerUri() {
			return new SupplierJwtDecoder(() -> JwtDecoders.fromIssuerLocation(this.properties.getIssuerUri()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnDefaultWebSecurity
	static class OAuth2SecurityFilterChainConfiguration {

		@Bean
		@ConditionalOnBean(JwtDecoder.class)
		SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
			http.authorizeRequests((requests) -> requests.anyRequest().authenticated());
			http.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
			return http.build();
		}

	}

}
