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
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.autoconfigure.security.oauth2.resource.IssuerUriCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.KeyValueCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.PropertyMapper;
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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
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
 * @author Yan Kardziyaka
 */
@Configuration(proxyBeanMethods = false)
class OAuth2ResourceServerJwtConfiguration {

	/**
	 * JwtDecoderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(JwtDecoder.class)
	static class JwtDecoderConfiguration {

		private final OAuth2ResourceServerProperties.Jwt properties;

		private final List<OAuth2TokenValidator<Jwt>> additionalValidators;

		/**
		 * Constructs a new JwtDecoderConfiguration with the specified
		 * OAuth2ResourceServerProperties and additionalValidators.
		 * @param properties the OAuth2ResourceServerProperties containing the JWT
		 * configuration properties
		 * @param additionalValidators the additional validators to be applied to the JWT
		 */
		JwtDecoderConfiguration(OAuth2ResourceServerProperties properties,
				ObjectProvider<OAuth2TokenValidator<Jwt>> additionalValidators) {
			this.properties = properties.getJwt();
			this.additionalValidators = additionalValidators.orderedStream().toList();
		}

		/**
		 * Creates a JwtDecoder bean based on the JWK Set URI property. The bean is
		 * conditionally created if the property
		 * "spring.security.oauth2.resourceserver.jwt.jwk-set-uri" is present.
		 * @param customizers the customizers for the JwkSetUriJwtDecoderBuilder
		 * @return the JwtDecoder bean
		 */
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

		/**
		 * Sets the supported JWS algorithms for the JwtDecoder.
		 * @param signatureAlgorithms the set of JWS signature algorithms to be supported
		 */
		private void jwsAlgorithms(Set<SignatureAlgorithm> signatureAlgorithms) {
			for (String algorithm : this.properties.getJwsAlgorithms()) {
				signatureAlgorithms.add(SignatureAlgorithm.from(algorithm));
			}
		}

		/**
		 * Returns the validators for the OAuth2 token.
		 * @param defaultValidator The default validator to be used.
		 * @return The validators for the OAuth2 token.
		 */
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

		/**
		 * Creates a JwtDecoder bean using a public key value.
		 * @return the JwtDecoder bean
		 * @throws Exception if an error occurs during the creation of the JwtDecoder
		 */
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

		/**
		 * Returns the key specification as a byte array.
		 * @param keyValue the key value to be processed
		 * @return the key specification as a byte array
		 */
		private byte[] getKeySpec(String keyValue) {
			keyValue = keyValue.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
			return Base64.getMimeDecoder().decode(keyValue);
		}

		/**
		 * Returns the exactly one JWS algorithm from the list of configured JWS
		 * algorithms.
		 * @return the exactly one JWS algorithm
		 * @throws IllegalStateException if the number of configured JWS algorithms is not
		 * exactly one
		 */
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

		/**
		 * Creates a SupplierJwtDecoder bean with conditional configuration based on the
		 * IssuerUriCondition. The SupplierJwtDecoder is responsible for decoding JWT
		 * tokens.
		 * @param customizers ObjectProvider of JwkSetUriJwtDecoderBuilderCustomizer to
		 * customize the JwkSetUriJwtDecoderBuilder
		 * @return SupplierJwtDecoder configured with the specified issuer URI
		 */
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

	/**
	 * OAuth2SecurityFilterChainConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnDefaultWebSecurity
	static class OAuth2SecurityFilterChainConfiguration {

		/**
		 * Configures the JWT security filter chain for OAuth2 authentication.
		 * @param http the HttpSecurity object to configure
		 * @return the configured SecurityFilterChain object
		 * @throws Exception if an error occurs during configuration
		 */
		@Bean
		@ConditionalOnBean(JwtDecoder.class)
		SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
			http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
			http.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
			return http.build();
		}

	}

	/**
	 * JwtConverterConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(JwtAuthenticationConverter.class)
	@Conditional(JwtConverterPropertiesCondition.class)
	static class JwtConverterConfiguration {

		private final OAuth2ResourceServerProperties.Jwt properties;

		/**
		 * Constructs a new JwtConverterConfiguration object with the specified
		 * OAuth2ResourceServerProperties.
		 * @param properties the OAuth2ResourceServerProperties containing the JWT
		 * configuration
		 */
		JwtConverterConfiguration(OAuth2ResourceServerProperties properties) {
			this.properties = properties.getJwt();
		}

		/**
		 * Returns a JwtAuthenticationConverter bean.
		 *
		 * This method creates a JwtGrantedAuthoritiesConverter object and sets its
		 * properties using the values from the JwtConverterConfiguration properties
		 * object. The authority prefix, authorities claim delimiter, and authorities
		 * claim name are set using the values from the properties object. The principal
		 * claim name is set using the value from the properties object.
		 *
		 * The JwtAuthenticationConverter object is then created and its principal claim
		 * name is set using the value from the properties object. The
		 * JwtGrantedAuthoritiesConverter object is set as the
		 * JwtAuthenticationConverter's JwtGrantedAuthoritiesConverter.
		 * @return JwtAuthenticationConverter - the JwtAuthenticationConverter bean
		 */
		@Bean
		JwtAuthenticationConverter getJwtAuthenticationConverter() {
			JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this.properties.getAuthorityPrefix()).to(grantedAuthoritiesConverter::setAuthorityPrefix);
			map.from(this.properties.getAuthoritiesClaimDelimiter())
				.to(grantedAuthoritiesConverter::setAuthoritiesClaimDelimiter);
			map.from(this.properties.getAuthoritiesClaimName())
				.to(grantedAuthoritiesConverter::setAuthoritiesClaimName);
			JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
			map.from(this.properties.getPrincipalClaimName()).to(jwtAuthenticationConverter::setPrincipalClaimName);
			jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
			return jwtAuthenticationConverter;
		}

	}

	/**
	 * JwtConverterPropertiesCondition class.
	 */
	private static class JwtConverterPropertiesCondition extends AnyNestedCondition {

		/**
		 * Constructs a new JwtConverterPropertiesCondition with the specified
		 * configuration phase.
		 * @param configurationPhase the configuration phase for the condition
		 */
		JwtConverterPropertiesCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		/**
		 * OnAuthorityPrefix class.
		 */
		@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "authority-prefix")
		static class OnAuthorityPrefix {

		}

		/**
		 * OnPrincipalClaimName class.
		 */
		@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "principal-claim-name")
		static class OnPrincipalClaimName {

		}

		/**
		 * OnAuthoritiesClaimName class.
		 */
		@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "authorities-claim-name")
		static class OnAuthoritiesClaimName {

		}

	}

}
