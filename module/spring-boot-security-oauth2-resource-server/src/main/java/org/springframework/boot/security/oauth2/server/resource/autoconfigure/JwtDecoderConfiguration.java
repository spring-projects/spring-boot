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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure;

import java.net.http.HttpClient;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
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
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder.PublicKeyJwtDecoderBuilder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * {@link Configuration @Configuration} for JWT decoder beans.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Mushtaq Ahmed
 * @author Roman Golovin
 * @author Yan Kardziyaka
 * @author Phillip Webb
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JwtDecoder.class)
@ConditionalOnMissingBean(JwtDecoder.class)
class JwtDecoderConfiguration {

	private static final Duration JWK_SET_CONNECT_TIMEOUT = Duration
		.ofMillis(JWKSourceBuilder.DEFAULT_HTTP_CONNECT_TIMEOUT);

	private static final Duration JWK_SET_READ_TIMEOUT = Duration.ofMillis(JWKSourceBuilder.DEFAULT_HTTP_READ_TIMEOUT);

	private final OAuth2ResourceServerProperties.Jwt properties;

	private final List<OAuth2TokenValidator<Jwt>> additionalValidators;

	private final ObjectProvider<JwkSetUriJwtDecoderBuilderCustomizer> jwkSetUriJwtDecoderBuilderCustomizers;

	private final @Nullable SslBundles sslBundles;

	JwtDecoderConfiguration(OAuth2ResourceServerProperties properties,
			ObjectProvider<OAuth2TokenValidator<Jwt>> additionalValidators,
			ObjectProvider<JwkSetUriJwtDecoderBuilderCustomizer> jwkSetUriJwtDecoderBuilderCustomizers,
			ObjectProvider<SslBundles> sslBundles) {
		this.properties = properties.getJwt();
		this.additionalValidators = additionalValidators.orderedStream().toList();
		this.jwkSetUriJwtDecoderBuilderCustomizers = jwkSetUriJwtDecoderBuilderCustomizers;
		this.sslBundles = sslBundles.getIfAvailable();
	}

	@Bean
	@ConditionalOnPublicKeyJwtDecoder
	JwtDecoder jwtDecoderByPublicKeyValue() throws Exception {
		PublicKeyJwtDecoderBuilder builder = NimbusJwtDecoder.withPublicKey(getReadPublicKey());
		builder.signatureAlgorithm(exactlyOneAlgorithm());
		NimbusJwtDecoder decoder = builder.build();
		decoder.setJwtValidator(getValidator());
		return decoder;
	}

	private RSAPublicKey getReadPublicKey() throws Exception {
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
					"spring.security.oauth2.resourceserver.jwt.jws-algorithms",
					StringUtils.collectionToCommaDelimitedString(algorithms), "Unknown algorithm");
		}
		return algorithm;
	}

	@Bean
	@Conditional(JwkSetUriCondition.class)
	JwtDecoder jwtDecoderByJwkKeySetUri() {
		String jwkSetUri = this.properties.getJwkSetUri();
		Assert.state(jwkSetUri != null, "No JWK Set URI property specified");
		JwkSetUriJwtDecoderBuilder builder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri);
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
	SupplierJwtDecoder jwtDecoderByIssuerUri() {
		return new SupplierJwtDecoder(this::supplyJwtDecoderByIssuerUri);
	}

	private JwtDecoder supplyJwtDecoderByIssuerUri() {
		String issuerUri = this.properties.getIssuerUri();
		Assert.state(issuerUri != null, "No JWT issuer URI propery specified");
		JwkSetUriJwtDecoderBuilder builder = NimbusJwtDecoder.withIssuerLocation(issuerUri);
		return buildJwkSetUriJwtDecoder(builder);
	}

	private JwtDecoder buildJwkSetUriJwtDecoder(JwkSetUriJwtDecoderBuilder builder) {
		this.jwkSetUriJwtDecoderBuilderCustomizers.orderedStream()
			.forEach((customizer) -> customizer.customize(builder));
		NimbusJwtDecoder decoder = builder.build();
		decoder.setJwtValidator(getValidator());
		return decoder;
	}

	private void configureSsl(JwkSetUriJwtDecoderBuilder builder) {
		SslBundle sslBundle = getSslBundle();
		if (sslBundle != null) {
			builder.restOperations(restOperations(sslBundle));
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

	private RestOperations restOperations(SslBundle sslBundle) {
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(createHttpClient(sslBundle));
		requestFactory.setReadTimeout(JWK_SET_READ_TIMEOUT);
		return new RestTemplate(requestFactory);
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
			validators.add(audienceValidator(this.properties.getAudiences()));
		}
		validators.addAll(this.additionalValidators);
		return validators.isEmpty() ? JwtValidators.createDefault()
				: JwtValidators.createDefaultWithValidators(validators);
	}

	private JwtClaimValidator<List<String>> audienceValidator(List<String> audiences) {
		return new JwtClaimValidator<>(JwtClaimNames.AUD, (claim) -> hasElementsInCommon(claim, audiences));
	}

	private <E> boolean hasElementsInCommon(@Nullable List<E> c1, List<E> c2) {
		return c1 != null && !Collections.disjoint(c1, c2);
	}

}
