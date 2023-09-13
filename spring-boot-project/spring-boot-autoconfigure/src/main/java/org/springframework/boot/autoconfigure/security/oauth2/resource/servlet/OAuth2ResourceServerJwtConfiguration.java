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

package org.springframework.boot.autoconfigure.security.oauth2.resource.servlet;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.CollectionUtils;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Configures a {@link JwtDecoder} when a JWK Set URI, OpenID Connect Issuer URI or Public
 * Key configuration is available. Also configures a {@link SecurityFilterChain} if a
 * {@link JwtDecoder} bean is found.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Mushtaq Ahmed
 * @author Roman Golovin
 */
@Configuration(proxyBeanMethods = false)
class OAuth2ResourceServerJwtConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(JwtDecoder.class)
	static class JwtDecoderConfiguration {

		private final OAuth2ResourceServerProperties.Jwt properties;

		private final List<OAuth2TokenValidator<Jwt>> additionalValidators;

		JwtDecoderConfiguration(OAuth2ResourceServerProperties properties,
				ObjectProvider<OAuth2TokenValidator<Jwt>> additionalValidators) {
			this.properties = properties.getJwt();
			this.additionalValidators = additionalValidators.orderedStream().toList();
		}

		@Bean
		@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
		JwtDecoder jwtDecoderByJwkKeySetUri(ObjectProvider<JwkSetUriJwtDecoderBuilderCustomizer> customizers) {
			JwkSetUriJwtDecoderBuilder builder = NimbusJwtDecoder.withJwkSetUri(this.properties.getJwkSetUri())
				.jwsAlgorithms(this::jwsAlgorithms);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			NimbusJwtDecoder nimbusJwtDecoder = builder.build();
			String issuerUri = this.properties.getIssuerUri();
			OAuth2TokenValidator<Jwt> defaultValidator = (issuerUri != null)
					? JwtValidators.createDefaultWithIssuer(issuerUri) : JwtValidators.createDefault();
			nimbusJwtDecoder.setJwtValidator(getValidators(defaultValidator));
			return nimbusJwtDecoder;
		}

		private void jwsAlgorithms(Set<SignatureAlgorithm> signatureAlgorithms) {
			for (String algorithm : this.properties.getJwsAlgorithms()) {
				signatureAlgorithms.add(SignatureAlgorithm.from(algorithm));
			}
		}

		private OAuth2TokenValidator<Jwt> getValidators(OAuth2TokenValidator<Jwt> defaultValidator) {
			List<String> audiences = this.properties.getAudiences();
			if (CollectionUtils.isEmpty(audiences) && this.additionalValidators.isEmpty()) {
				return defaultValidator;
			}
			List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
			validators.add(defaultValidator);
			if (!CollectionUtils.isEmpty(audiences)) {
				validators.add(new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
						(aud) -> aud != null && !Collections.disjoint(aud, audiences)));
			}
			validators.addAll(this.additionalValidators);
			return new DelegatingOAuth2TokenValidator<>(validators);
		}

		@Bean
		@Conditional(KeyValueCondition.class)
		JwtDecoder jwtDecoderByPublicKeyValue() throws Exception {
			RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
				.generatePublic(new X509EncodedKeySpec(getKeySpec(this.properties.readPublicKey())));
			NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(publicKey)
				.signatureAlgorithm(SignatureAlgorithm.from(exactlyOneAlgorithm()))
				.build();
			jwtDecoder.setJwtValidator(getValidators(JwtValidators.createDefault()));
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
		SupplierJwtDecoder jwtDecoderByIssuerUri(ObjectProvider<JwkSetUriJwtDecoderBuilderCustomizer> customizers) {
			return new SupplierJwtDecoder(() -> {
				String issuerUri = this.properties.getIssuerUri();
				JwkSetUriJwtDecoderBuilder builder = NimbusJwtDecoder.withIssuerLocation(issuerUri);
				customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
				NimbusJwtDecoder jwtDecoder = builder.build();
				jwtDecoder.setJwtValidator(getValidators(JwtValidators.createDefaultWithIssuer(issuerUri)));
				return jwtDecoder;
			});
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnDefaultWebSecurity
	static class OAuth2SecurityFilterChainConfiguration {

		@Bean
		@ConditionalOnBean(JwtDecoder.class)
		SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
			http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
			http.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
			return http.build();
		}

	}

}
