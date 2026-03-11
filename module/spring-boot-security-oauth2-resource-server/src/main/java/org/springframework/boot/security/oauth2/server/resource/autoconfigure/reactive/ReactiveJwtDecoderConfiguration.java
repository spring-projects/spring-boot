/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.ConditionalOnIssuerLocationJwtDecoder;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.ConditionalOnPublicKeyJwtDecoder;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder.JwkSetUriReactiveJwtDecoderBuilder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder.PublicKeyReactiveJwtDecoderBuilder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierReactiveJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * {@link Configuration @Configuration} for reactive JWT decoder beans.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Anastasiia Losieva
 * @author Mushtaq Ahmed
 * @author Roman Golovin
 * @author Yan Kardziyaka
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(ReactiveJwtDecoder.class)
class ReactiveJwtDecoderConfiguration {

	private final OAuth2ResourceServerProperties.Jwt properties;

	private final List<OAuth2TokenValidator<Jwt>> additionalValidators;

	private final ObjectProvider<JwkSetUriReactiveJwtDecoderBuilderCustomizer> jwkSetUriReactiveJwtDecoderBuilderCustomizers;

	ReactiveJwtDecoderConfiguration(OAuth2ResourceServerProperties properties,
			ObjectProvider<OAuth2TokenValidator<Jwt>> additionalValidators,
			ObjectProvider<JwkSetUriReactiveJwtDecoderBuilderCustomizer> jwkSetUriReactiveJwtDecoderBuilderCustomizers) {
		this.properties = properties.getJwt();
		this.additionalValidators = additionalValidators.orderedStream().toList();
		this.jwkSetUriReactiveJwtDecoderBuilderCustomizers = jwkSetUriReactiveJwtDecoderBuilderCustomizers;
	}

	@Bean
	@ConditionalOnPublicKeyJwtDecoder
	NimbusReactiveJwtDecoder reactiveJwtDecoderByPublicKeyValue() throws Exception {
		RSAPublicKey publicKey = getReadPublicKey();
		PublicKeyReactiveJwtDecoderBuilder builder = NimbusReactiveJwtDecoder.withPublicKey(publicKey);
		builder.signatureAlgorithm(SignatureAlgorithm.from(exactlyOneAlgorithm()));
		NimbusReactiveJwtDecoder decoder = builder.build();
		decoder.setJwtValidator(getValidator());
		return decoder;
	}

	private RSAPublicKey getReadPublicKey() throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
		X509EncodedKeySpec spec = new X509EncodedKeySpec(decodeKeyProperty(this.properties.readPublicKey()));
		return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
	}

	private byte[] decodeKeyProperty(String value) {
		return Base64.getMimeDecoder()
			.decode(value.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", ""));
	}

	private String exactlyOneAlgorithm() {
		List<String> algorithms = this.properties.getJwsAlgorithms();
		Assert.state(algorithms != null && algorithms.size() == 1,
				() -> "Creating a JWT decoder using a public key requires exactly one JWS algorithm but "
						+ algorithms.size() + " were configured");
		return algorithms.get(0);
	}

	@Bean
	@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri")
	ReactiveJwtDecoder reactiveJwtDecoderByJwkKeySetUri() {
		JwkSetUriReactiveJwtDecoderBuilder builder = NimbusReactiveJwtDecoder
			.withJwkSetUri(this.properties.getJwkSetUri());
		builder.jwsAlgorithms(this::jwsAlgorithms);
		return buildJwkSetUriJwtDecoder(builder);
	}

	private void jwsAlgorithms(Set<SignatureAlgorithm> signatureAlgorithms) {
		this.properties.getJwsAlgorithms().stream().map(SignatureAlgorithm::from).forEach(signatureAlgorithms::add);
	}

	@Bean
	@ConditionalOnIssuerLocationJwtDecoder
	SupplierReactiveJwtDecoder reactiveJwtDecoderByIssuerUri() {
		return new SupplierReactiveJwtDecoder(this::supplyJwtDecoderByIssuerUri);
	}

	private ReactiveJwtDecoder supplyJwtDecoderByIssuerUri() {
		String issuerUri = this.properties.getIssuerUri();
		Assert.state(issuerUri != null, "No JWT issuer URI propery specified");
		JwkSetUriReactiveJwtDecoderBuilder builder = NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri);
		return buildJwkSetUriJwtDecoder(builder);
	}

	private ReactiveJwtDecoder buildJwkSetUriJwtDecoder(JwkSetUriReactiveJwtDecoderBuilder builder) {
		this.jwkSetUriReactiveJwtDecoderBuilderCustomizers.orderedStream()
			.forEach((customizer) -> customizer.customize(builder));
		NimbusReactiveJwtDecoder decoder = builder.build();
		decoder.setJwtValidator(getValidator());
		return decoder;
	}

	private OAuth2TokenValidator<Jwt> getValidator() {
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		if (this.properties.getIssuerUri() != null) {
			validators.add(new JwtIssuerValidator(this.properties.getIssuerUri()));
		}
		if (!CollectionUtils.isEmpty(this.properties.getAudiences())) {
			validators.add(audValidator(this.properties.getAudiences()));
		}
		validators.addAll(this.additionalValidators);
		return validators.isEmpty() ? JwtValidators.createDefault()
				: JwtValidators.createDefaultWithValidators(validators);
	}

	private JwtClaimValidator<List<String>> audValidator(List<String> audiences) {
		return new JwtClaimValidator<>(JwtClaimNames.AUD, (claim) -> hasElementsInCommon(claim, audiences));
	}

	private <E> boolean hasElementsInCommon(@Nullable List<E> c1, List<E> c2) {
		return c1 != null && !Collections.disjoint(c1, c2);
	}

}
