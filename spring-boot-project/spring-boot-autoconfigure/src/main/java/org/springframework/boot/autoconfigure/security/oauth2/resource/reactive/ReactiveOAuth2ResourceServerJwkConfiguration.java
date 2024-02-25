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

package org.springframework.boot.autoconfigure.security.oauth2.resource.reactive;

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
import org.springframework.boot.autoconfigure.security.oauth2.resource.IssuerUriCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.KeyValueCondition;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.PropertyMapper;
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
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder.JwkSetUriReactiveJwtDecoderBuilder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
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
 * @author Roman Golovin
 * @author Yan Kardziyaka
 */
@Configuration(proxyBeanMethods = false)
class ReactiveOAuth2ResourceServerJwkConfiguration {

	/**
	 * JwtConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ReactiveJwtDecoder.class)
	static class JwtConfiguration {

		private final OAuth2ResourceServerProperties.Jwt properties;

		private final List<OAuth2TokenValidator<Jwt>> additionalValidators;

		/**
		 * Constructs a new JwtConfiguration object with the provided
		 * OAuth2ResourceServerProperties and additionalValidators.
		 * @param properties the OAuth2ResourceServerProperties object containing JWT
		 * configuration properties
		 * @param additionalValidators the additional OAuth2TokenValidators to be used for
		 * validating JWT tokens
		 */
		JwtConfiguration(OAuth2ResourceServerProperties properties,
				ObjectProvider<OAuth2TokenValidator<Jwt>> additionalValidators) {
			this.properties = properties.getJwt();
			this.additionalValidators = additionalValidators.orderedStream().toList();
		}

		/**
		 * Creates a ReactiveJwtDecoder bean based on the provided properties. This bean
		 * is conditionally created if the property
		 * "spring.security.oauth2.resourceserver.jwt.jwk-set-uri" is present.
		 * @param customizers ObjectProvider of
		 * JwkSetUriReactiveJwtDecoderBuilderCustomizer to customize the builder
		 * @return ReactiveJwtDecoder bean
		 */
		@Bean
		@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
		ReactiveJwtDecoder jwtDecoder(ObjectProvider<JwkSetUriReactiveJwtDecoderBuilderCustomizer> customizers) {
			JwkSetUriReactiveJwtDecoderBuilder builder = NimbusReactiveJwtDecoder
				.withJwkSetUri(this.properties.getJwkSetUri())
				.jwsAlgorithms(this::jwsAlgorithms);
			customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			NimbusReactiveJwtDecoder nimbusReactiveJwtDecoder = builder.build();
			String issuerUri = this.properties.getIssuerUri();
			OAuth2TokenValidator<Jwt> defaultValidator = (issuerUri != null)
					? JwtValidators.createDefaultWithIssuer(issuerUri) : JwtValidators.createDefault();
			nimbusReactiveJwtDecoder.setJwtValidator(getValidators(defaultValidator));
			return nimbusReactiveJwtDecoder;
		}

		/**
		 * Sets the supported JWS algorithms for JWT configuration.
		 * @param signatureAlgorithms a set of SignatureAlgorithm objects representing the
		 * supported JWS algorithms
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
		 * Creates a NimbusReactiveJwtDecoder bean with a public key value. This bean is
		 * conditionally created based on the KeyValueCondition.
		 * @return the created NimbusReactiveJwtDecoder bean
		 * @throws Exception if an error occurs during the creation of the public key
		 */
		@Bean
		@Conditional(KeyValueCondition.class)
		NimbusReactiveJwtDecoder jwtDecoderByPublicKeyValue() throws Exception {
			RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
				.generatePublic(new X509EncodedKeySpec(getKeySpec(this.properties.readPublicKey())));
			NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withPublicKey(publicKey)
				.signatureAlgorithm(SignatureAlgorithm.from(exactlyOneAlgorithm()))
				.build();
			jwtDecoder.setJwtValidator(getValidators(JwtValidators.createDefault()));
			return jwtDecoder;
		}

		/**
		 * Returns the key specification as a byte array.
		 * @param keyValue the key value in PEM format
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
		 * Creates a SupplierReactiveJwtDecoder bean with a conditional annotation based
		 * on the IssuerUriCondition class. This method is responsible for configuring the
		 * JWT decoder by issuer URI. It uses a
		 * JwkSetUriReactiveJwtDecoderBuilderCustomizer object provider to customize the
		 * builder. The builder is initialized with the issuer URI obtained from the
		 * properties. Customizers are applied to the builder in the order specified by
		 * their priority. Finally, a NimbusReactiveJwtDecoder is built using the builder
		 * and a JWT validator is set on the decoder. The JWT validator is created using
		 * the default validators with the issuer URI from the properties. The
		 * SupplierReactiveJwtDecoder is then returned as the result of this method.
		 * @param customizers the object provider for
		 * JwkSetUriReactiveJwtDecoderBuilderCustomizer
		 * @return the SupplierReactiveJwtDecoder bean
		 */
		@Bean
		@Conditional(IssuerUriCondition.class)
		SupplierReactiveJwtDecoder jwtDecoderByIssuerUri(
				ObjectProvider<JwkSetUriReactiveJwtDecoderBuilderCustomizer> customizers) {
			return new SupplierReactiveJwtDecoder(() -> {
				JwkSetUriReactiveJwtDecoderBuilder builder = NimbusReactiveJwtDecoder
					.withIssuerLocation(this.properties.getIssuerUri());
				customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
				NimbusReactiveJwtDecoder jwtDecoder = builder.build();
				jwtDecoder.setJwtValidator(
						getValidators(JwtValidators.createDefaultWithIssuer(this.properties.getIssuerUri())));
				return jwtDecoder;
			});
		}

	}

	/**
	 * JwtConverterConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ReactiveJwtAuthenticationConverter.class)
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
		 * Creates a ReactiveJwtAuthenticationConverter bean.
		 *
		 * This method initializes a ReactiveJwtAuthenticationConverter bean by
		 * configuring its properties based on the values provided in the
		 * JwtConverterConfiguration class.
		 * @return The created ReactiveJwtAuthenticationConverter bean.
		 */
		@Bean
		ReactiveJwtAuthenticationConverter reactiveJwtAuthenticationConverter() {
			JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this.properties.getAuthorityPrefix()).to(grantedAuthoritiesConverter::setAuthorityPrefix);
			map.from(this.properties.getAuthoritiesClaimDelimiter())
				.to(grantedAuthoritiesConverter::setAuthoritiesClaimDelimiter);
			map.from(this.properties.getAuthoritiesClaimName())
				.to(grantedAuthoritiesConverter::setAuthoritiesClaimName);
			ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
			map.from(this.properties.getPrincipalClaimName()).to(jwtAuthenticationConverter::setPrincipalClaimName);
			jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
					new ReactiveJwtGrantedAuthoritiesConverterAdapter(grantedAuthoritiesConverter));
			return jwtAuthenticationConverter;
		}

	}

	/**
	 * WebSecurityConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(SecurityWebFilterChain.class)
	static class WebSecurityConfiguration {

		/**
		 * Configures the security filter chain for the Spring WebFlux application.
		 * @param http The ServerHttpSecurity object used to configure the security filter
		 * chain.
		 * @param jwtDecoder The ReactiveJwtDecoder object used for decoding JWT tokens.
		 * @return The configured SecurityWebFilterChain object.
		 */
		@Bean
		@ConditionalOnBean(ReactiveJwtDecoder.class)
		SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http, ReactiveJwtDecoder jwtDecoder) {
			http.authorizeExchange((exchanges) -> exchanges.anyExchange().authenticated());
			http.oauth2ResourceServer((server) -> customDecoder(server, jwtDecoder));
			return http.build();
		}

		/**
		 * Configures a custom decoder for decoding JWT tokens in the OAuth2 resource
		 * server.
		 * @param server the OAuth2 resource server specification
		 * @param decoder the custom JWT decoder
		 */
		private void customDecoder(OAuth2ResourceServerSpec server, ReactiveJwtDecoder decoder) {
			server.jwt((jwt) -> jwt.jwtDecoder(decoder));
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
