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
import java.net.http.HttpClient;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLParameters;

import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.ConditionalOnIssuerLocationJwtDecoder;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.ConditionalOnPublicKeyJwtDecoder;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

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
@ConditionalOnClass(WebClient.class)
@ConditionalOnMissingBean(ReactiveJwtDecoder.class)
class ReactiveJwtDecoderConfiguration {

	private static final Duration JWK_SET_CONNECT_TIMEOUT = Duration
		.ofMillis(JWKSourceBuilder.DEFAULT_HTTP_CONNECT_TIMEOUT);

	private static final Duration JWK_SET_READ_TIMEOUT = Duration.ofMillis(JWKSourceBuilder.DEFAULT_HTTP_READ_TIMEOUT);

	private final OAuth2ResourceServerProperties.Jwt properties;

	private final List<OAuth2TokenValidator<Jwt>> additionalValidators;

	private final ObjectProvider<JwkSetUriReactiveJwtDecoderBuilderCustomizer> jwkSetUriReactiveJwtDecoderBuilderCustomizers;

	private final @Nullable SslBundles sslBundles;

	ReactiveJwtDecoderConfiguration(OAuth2ResourceServerProperties properties,
			ObjectProvider<OAuth2TokenValidator<Jwt>> additionalValidators,
			ObjectProvider<JwkSetUriReactiveJwtDecoderBuilderCustomizer> jwkSetUriReactiveJwtDecoderBuilderCustomizers,
			ObjectProvider<SslBundles> sslBundles) {
		this.properties = properties.getJwt();
		this.additionalValidators = additionalValidators.orderedStream().toList();
		this.jwkSetUriReactiveJwtDecoderBuilderCustomizers = jwkSetUriReactiveJwtDecoderBuilderCustomizers;
		this.sslBundles = sslBundles.getIfAvailable();
	}

	@Bean
	@ConditionalOnPublicKeyJwtDecoder
	NimbusReactiveJwtDecoder reactiveJwtDecoderByPublicKeyValue() throws Exception {
		RSAPublicKey publicKey = getReadPublicKey();
		PublicKeyReactiveJwtDecoderBuilder builder = NimbusReactiveJwtDecoder.withPublicKey(publicKey);
		builder.signatureAlgorithm(exactlyOneAlgorithm());
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

	private SignatureAlgorithm exactlyOneAlgorithm() {
		List<String> algorithms = this.properties.getJwsAlgorithms();
		Assert.state(algorithms != null && algorithms.size() == 1,
				() -> "Creating a JWT decoder using a public key requires exactly one JWS algorithm but "
						+ algorithms.size() + " were configured");
		SignatureAlgorithm algorithm = SignatureAlgorithm.from(algorithms.get(0));
		if (algorithm == null) {
			throw new InvalidConfigurationPropertyValueException(
					"spring.security.oauth2.resourceserver.jwt.jws-algorithms", algorithms.get(0), "Unknown algorithm");
		}
		return algorithm;
	}

	@Bean
	@Conditional(JwkSetUriCondition.class)
	ReactiveJwtDecoder reactiveJwtDecoderByJwkKeySetUri() {
		String jwkSetUri = this.properties.getJwkSetUri();
		Assert.notNull(jwkSetUri, "No JWK Set URI specified");
		JwkSetUriReactiveJwtDecoderBuilder builder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri);
		builder.jwsAlgorithms(this::jwsAlgorithms);
		configureSsl(builder);
		return buildJwkSetUriJwtDecoder(builder);
	}

	private void jwsAlgorithms(Set<SignatureAlgorithm> signatureAlgorithms) {
		for (String algorithm : this.properties.getJwsAlgorithms()) {
			SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.from(algorithm);
			if (signatureAlgorithm == null) {
				throw new InvalidConfigurationPropertyValueException(
						"spring.security.oauth2.resourceserver.jwt.jws-algorithms", algorithm, "Unknown algorithm");
			}
			signatureAlgorithms.add(signatureAlgorithm);
		}
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

	private void configureSsl(JwkSetUriReactiveJwtDecoderBuilder builder) {
		SslBundle sslBundle = getSslBundle();
		if (sslBundle != null) {
			builder.webClient(webClient(sslBundle));
		}
	}

	private @Nullable SslBundle getSslBundle() {
		OAuth2ResourceServerProperties.Jwkset.Ssl ssl = this.properties.getJwkset().getSsl();
		if (!ssl.isEnabled()) {
			return null;
		}
		String bundleName = ssl.getBundle();
		if (StringUtils.hasLength(bundleName)) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(bundleName);
		}
		return SslBundle.systemDefault();
	}

	private WebClient webClient(SslBundle sslBundle) {
		JdkClientHttpConnector connector = new JdkClientHttpConnector(createHttpClient(sslBundle));
		connector.setReadTimeout(JWK_SET_READ_TIMEOUT);
		return WebClient.builder().clientConnector(connector).build();
	}

	private HttpClient createHttpClient(SslBundle sslBundle) {
		return HttpClient.newBuilder()
			.connectTimeout(JWK_SET_CONNECT_TIMEOUT)
			.sslContext(sslBundle.createSslContext())
			.sslParameters(asSslParameters(sslBundle))
			.build();
	}

	private SSLParameters asSslParameters(SslBundle sslBundle) {
		SslOptions options = sslBundle.getOptions();
		SSLParameters parameters = new SSLParameters();
		parameters.setCipherSuites(options.getCiphers());
		parameters.setProtocols(options.getEnabledProtocols());
		return parameters;
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
