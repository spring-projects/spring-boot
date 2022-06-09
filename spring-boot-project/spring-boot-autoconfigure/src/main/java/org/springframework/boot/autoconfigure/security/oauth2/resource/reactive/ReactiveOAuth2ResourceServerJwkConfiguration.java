/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource.reactive;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.IssuerUriCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.KeyValueCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.OAuth2ResourceServerSpec;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.oauth2.jwt.SupplierReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.CollectionUtils;

/**
 * Configures a {@link ReactiveJwtDecoder} when a JWK Set URI, OpenID Connect Issuer URI
 * or Public Key configuration is available. Also configures a
 * {@link SecurityWebFilterChain} if a {@link ReactiveJwtDecoder} bean is found.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Anastasiia Losieva
 * @author Mushtaq Ahmed
 */
@Configuration(proxyBeanMethods = false)
class ReactiveOAuth2ResourceServerJwkConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ReactiveJwtDecoder.class)
	static class JwtConfiguration {

		private final OAuth2ResourceServerProperties.Jwt properties;

		JwtConfiguration(OAuth2ResourceServerProperties properties) {
			this.properties = properties.getJwt();
		}

		@Bean
		@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
		ReactiveJwtDecoder jwtDecoder() {
			NimbusReactiveJwtDecoder nimbusReactiveJwtDecoder = NimbusReactiveJwtDecoder
					.withJwkSetUri(this.properties.getJwkSetUri()).jwsAlgorithms(this::jwsAlgorithms).build();
			String issuerUri = this.properties.getIssuerUri();
			Supplier<OAuth2TokenValidator<Jwt>> defaultValidator = (issuerUri != null)
					? () -> JwtValidators.createDefaultWithIssuer(issuerUri) : JwtValidators::createDefault;
			nimbusReactiveJwtDecoder.setJwtValidator(getValidators(defaultValidator));
			return nimbusReactiveJwtDecoder;
		}

		private void jwsAlgorithms(Set<SignatureAlgorithm> signatureAlgorithms) {
			for (String algorithm : this.properties.getJwsAlgorithms()) {
				signatureAlgorithms.add(SignatureAlgorithm.from(algorithm));
			}
		}

		private OAuth2TokenValidator<Jwt> getValidators(Supplier<OAuth2TokenValidator<Jwt>> defaultValidator) {
			OAuth2TokenValidator<Jwt> defaultValidators = defaultValidator.get();
			List<String> audiences = this.properties.getAudiences();
			if (CollectionUtils.isEmpty(audiences)) {
				return defaultValidators;
			}
			List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
			validators.add(defaultValidators);
			validators.add(new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
					(aud) -> aud != null && !Collections.disjoint(aud, audiences)));
			return new DelegatingOAuth2TokenValidator<>(validators);
		}

		@Bean
		@Conditional(KeyValueCondition.class)
		NimbusReactiveJwtDecoder jwtDecoderByPublicKeyValue() throws Exception {
			RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(getKeySpec(this.properties.readPublicKey())));
			NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey)
					.signatureAlgorithm(SignatureAlgorithm.from(exactlyOneAlgorithm())).build();
			jwtDecoder.setJwtValidator(getValidators(JwtValidators::createDefault));
			return jwtDecoder;
		}

		private byte[] getKeySpec(String keyValue) {
			keyValue = keyValue.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
			return Base64.getMimeDecoder().decode(keyValue);
		}

		private String exactlyOneAlgorithm() {
			List<String> algorithms = this.properties.getJwsAlgorithms();
			int count = (algorithms != null) ? algorithms.size() : 0;
			if (count != 1) {
				throw new IllegalStateException(
						"Creating a JWT decoder using a public key requires exactly one JWS algorithm but " + count
								+ " were configured");
			}
			return algorithms.get(0);
		}

		@Bean
		@Conditional(IssuerUriCondition.class)
		SupplierReactiveJwtDecoder jwtDecoderByIssuerUri() {
			return new SupplierReactiveJwtDecoder(() -> {
				NimbusReactiveJwtDecoder jwtDecoder = (NimbusReactiveJwtDecoder) ReactiveJwtDecoders
						.fromIssuerLocation(this.properties.getIssuerUri());
				jwtDecoder.setJwtValidator(
						getValidators(() -> JwtValidators.createDefaultWithIssuer(this.properties.getIssuerUri())));
				return jwtDecoder;
			});
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(SecurityWebFilterChain.class)
	static class WebSecurityConfiguration {

		@Bean
		@ConditionalOnBean(ReactiveJwtDecoder.class)
		SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
			http.authorizeExchange((exchanges) -> exchanges.anyExchange().authenticated());
			http.oauth2ResourceServer((server) -> customDecoder(server, jwtDecoder));
			return http.build();
		}

		private void customDecoder(OAuth2ResourceServerSpec server, ReactiveJwtDecoder decoder) {
			server.jwt((jwt) -> jwt.jwtDecoder(decoder));
		}

	}

}
