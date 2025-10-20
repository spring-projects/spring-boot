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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.nimbusds.jose.JWSAlgorithm;
import jakarta.servlet.Filter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ThrowingConsumer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.JwtConverterCustomizationsArgumentsProvider;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OAuth2ResourceServerAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Mushtaq Ahmed
 * @author Roman Golovin
 * @author Yan Kardziyaka
 */
class OAuth2ResourceServerAutoConfigurationTests {

	private static final String MANAGEMENT_SECURITY_FILTER_CHAIN_BEAN = "managementSecurityFilterChain";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class))
		.withUserConfiguration(TestConfig.class);

	private @Nullable MockWebServer server;

	private static final String JWK_SET = "{\"keys\":[{\"kty\":\"RSA\",\"e\":\"AQAB\",\"use\":\"sig\","
			+ "\"kid\":\"one\",\"n\":\"oXJ8OyOv_eRnce4akdanR4KYRfnC2zLV4uYNQpcFn6oHL0dj7D6kxQmsXoYgJV8ZVDn71KGm"
			+ "uLvolxsDncc2UrhyMBY6DVQVgMSVYaPCTgW76iYEKGgzTEw5IBRQL9w3SRJWd3VJTZZQjkXef48Ocz06PGF3lhbz4t5UEZtd"
			+ "F4rIe7u-977QwHuh7yRPBQ3sII-cVoOUMgaXB9SHcGF2iZCtPzL_IffDUcfhLQteGebhW8A6eUHgpD5A1PQ-JCw_G7UOzZAj"
			+ "jDjtNM2eqm8j-Ms_gqnm4MiCZ4E-9pDN77CAAPVN7kuX6ejs9KBXpk01z48i9fORYk9u7rAkh1HuQw\"}]}";

	@AfterEach
	void cleanup() throws Exception {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	@Test
	void autoConfigurationShouldConfigureResourceServer() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.run((context) -> {
				assertThat(context).hasSingleBean(JwtDecoder.class);
				assertThat(getBearerTokenFilter(context)).isNotNull();
				assertJwkSetUriJwtDecoderBuilderCustomization(context);
			});
	}

	private void assertJwkSetUriJwtDecoderBuilderCustomization(AssertableWebApplicationContext context) {
		JwkSetUriJwtDecoderBuilderCustomizer customizer = context.getBean("decoderBuilderCustomizer",
				JwkSetUriJwtDecoderBuilderCustomizer.class);
		JwkSetUriJwtDecoderBuilderCustomizer anotherCustomizer = context.getBean("anotherDecoderBuilderCustomizer",
				JwkSetUriJwtDecoderBuilderCustomizer.class);
		InOrder inOrder = inOrder(customizer, anotherCustomizer);
		inOrder.verify(customizer).customize(any());
		inOrder.verify(anotherCustomizer).customize(any());
	}

	@Test
	void autoConfigurationShouldMatchDefaultJwsAlgorithm() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.run((context) -> {
				JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
				assertThat(jwtDecoder).extracting("jwtProcessor.jwsKeySelector.jwsAlgs")
					.asInstanceOf(InstanceOfAssertFactories.collection(JWSAlgorithm.class))
					.containsExactlyInAnyOrder(JWSAlgorithm.RS256);
			});
	}

	@Test
	void autoConfigurationShouldConfigureResourceServerWithSingleJwsAlgorithm() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.jwt.jws-algorithms=RS384")
			.run((context) -> {
				JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
				assertThat(jwtDecoder).extracting("jwtProcessor.jwsKeySelector.jwsAlgs")
					.asInstanceOf(InstanceOfAssertFactories.collection(JWSAlgorithm.class))
					.containsExactlyInAnyOrder(JWSAlgorithm.RS384);
				assertThat(getBearerTokenFilter(context)).isNotNull();
			});
	}

	@Test
	void autoConfigurationShouldConfigureResourceServerWithMultipleJwsAlgorithms() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.jwt.jws-algorithms=RS256, RS384, RS512")
			.run((context) -> {
				JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
				assertThat(jwtDecoder).extracting("jwtProcessor.jwsKeySelector.jwsAlgs")
					.asInstanceOf(InstanceOfAssertFactories.collection(JWSAlgorithm.class))
					.containsExactlyInAnyOrder(JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512);
				assertThat(getBearerTokenFilter(context)).isNotNull();
			});
	}

	@Test
	@WithPublicKeyResource
	void autoConfigurationUsingPublicKeyValueShouldConfigureResourceServerUsingSingleJwsAlgorithm() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location",
					"spring.security.oauth2.resourceserver.jwt.jws-algorithms=RS384")
			.run((context) -> {
				NimbusJwtDecoder nimbusJwtDecoder = context.getBean(NimbusJwtDecoder.class);
				assertThat(nimbusJwtDecoder).extracting("jwtProcessor.jwsKeySelector.expectedJWSAlg")
					.isEqualTo(JWSAlgorithm.RS384);
			});
	}

	@Test
	@WithPublicKeyResource
	void autoConfigurationUsingPublicKeyValueWithMultipleJwsAlgorithmsShouldFail() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location",
					"spring.security.oauth2.resourceserver.jwt.jws-algorithms=RSA256,RS384")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasRootCauseMessage(
						"Creating a JWT decoder using a public key requires exactly one JWS algorithm but 2 were "
								+ "configured");
			});
	}

	@SuppressWarnings("unchecked")
	@Test
	void autoConfigurationShouldConfigureResourceServerUsingOidcIssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://"
					+ this.server.getHostName() + ":" + this.server.getPort() + "/" + path)
			.run((context) -> {
				assertThat(context).hasSingleBean(SupplierJwtDecoder.class);
				assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
				SupplierJwtDecoder supplierJwtDecoderBean = context.getBean(SupplierJwtDecoder.class);
				Supplier<JwtDecoder> jwtDecoderSupplier = (Supplier<JwtDecoder>) ReflectionTestUtils
					.getField(supplierJwtDecoderBean, "delegate");
				assertThat(jwtDecoderSupplier).isNotNull();
				jwtDecoderSupplier.get();
				assertJwkSetUriJwtDecoderBuilderCustomization(context);
			});
		// The last request is to the JWK Set endpoint to look up the algorithm
		assertThat(this.server.getRequestCount()).isEqualTo(2);
	}

	@SuppressWarnings("unchecked")
	@Test
	void autoConfigurationShouldConfigureResourceServerUsingOidcRfc8414IssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponsesWithErrors(cleanIssuerPath, 1);
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://"
					+ this.server.getHostName() + ":" + this.server.getPort() + "/" + path)
			.run((context) -> {
				assertThat(context).hasSingleBean(SupplierJwtDecoder.class);
				assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
				SupplierJwtDecoder supplierJwtDecoderBean = context.getBean(SupplierJwtDecoder.class);
				Supplier<JwtDecoder> jwtDecoderSupplier = (Supplier<JwtDecoder>) ReflectionTestUtils
					.getField(supplierJwtDecoderBean, "delegate");
				assertThat(jwtDecoderSupplier).isNotNull();
				jwtDecoderSupplier.get();
				assertJwkSetUriJwtDecoderBuilderCustomization(context);
			});
		// The last request is to the JWK Set endpoint to look up the algorithm
		assertThat(this.server.getRequestCount()).isEqualTo(3);
	}

	@SuppressWarnings("unchecked")
	@Test
	void autoConfigurationShouldConfigureResourceServerUsingOAuthIssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponsesWithErrors(cleanIssuerPath, 2);

		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://"
					+ this.server.getHostName() + ":" + this.server.getPort() + "/" + path)
			.run((context) -> {
				assertThat(context).hasSingleBean(SupplierJwtDecoder.class);
				assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
				SupplierJwtDecoder supplierJwtDecoderBean = context.getBean(SupplierJwtDecoder.class);
				Supplier<JwtDecoder> jwtDecoderSupplier = (Supplier<JwtDecoder>) ReflectionTestUtils
					.getField(supplierJwtDecoderBean, "delegate");
				assertThat(jwtDecoderSupplier).isNotNull();
				jwtDecoderSupplier.get();
				assertJwkSetUriJwtDecoderBuilderCustomization(context);
			});
		// The last request is to the JWK Set endpoint to look up the algorithm
		assertThat(this.server.getRequestCount()).isEqualTo(4);
	}

	@Test
	@WithPublicKeyResource
	void autoConfigurationShouldConfigureResourceServerUsingPublicKeyValue() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location")
			.run((context) -> {
				assertThat(context).hasSingleBean(JwtDecoder.class);
				assertThat(getBearerTokenFilter(context)).isNotNull();
			});
	}

	@Test
	void autoConfigurationShouldFailIfPublicKeyLocationDoesNotExist() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:does-not-exist")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining("class path resource [does-not-exist]")
				.hasMessageContaining("Public key location does not exist"));
	}

	@Test
	@WithPublicKeyResource
	void autoConfigurationShouldFailIfAlgorithmIsInvalid() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location",
					"spring.security.oauth2.resourceserver.jwt.jws-algorithms=NOT_VALID")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining("signatureAlgorithm cannot be null"));
	}

	@Test
	void autoConfigurationWhenSetUriKeyLocationAndIssuerUriPresentShouldUseSetUri() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer-uri.com",
					"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location",
					"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.run((context) -> {
				assertThat(context).hasSingleBean(JwtDecoder.class);
				assertThat(getBearerTokenFilter(context)).isNotNull();
				assertThat(context.containsBean("jwtDecoderByJwkKeySetUri")).isTrue();
				assertThat(context.containsBean("jwtDecoderByIssuerUri")).isFalse();
			});
	}

	@Test
	void autoConfigurationWhenKeyLocationAndIssuerUriPresentShouldUseIssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://" + this.server.getHostName() + ":"
							+ this.server.getPort(),
					"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location")
			.run((context) -> {
				assertThat(context).hasSingleBean(JwtDecoder.class);
				assertThat(getBearerTokenFilter(context)).isNotNull();
				assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
			});
	}

	@Test
	void autoConfigurationWhenJwkSetUriNullShouldNotFail() {
		this.contextRunner.run((context) -> assertThat(getBearerTokenFilter(context)).isNull());
	}

	@Test
	void jwtDecoderByJwkSetUriIsConditionalOnMissingBean() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.withUserConfiguration(JwtDecoderConfig.class)
			.run((context) -> assertThat(getBearerTokenFilter(context)).isNotNull());
	}

	@Test
	void jwtDecoderByOidcIssuerUriIsConditionalOnMissingBean() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.jwt.issuer-uri=https://jwk-oidc-issuer-location.com")
			.withUserConfiguration(JwtDecoderConfig.class)
			.run((context) -> assertThat(getBearerTokenFilter(context)).isNotNull());
	}

	@Test
	void autoConfigurationShouldBeConditionalOnResourceServerClass() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.withUserConfiguration(JwtDecoderConfig.class)
			.withClassLoader(new FilteredClassLoader(BearerTokenAuthenticationToken.class))
			.run((context) -> {
				assertThat(context).doesNotHaveBean(OAuth2ResourceServerAutoConfiguration.class);
				assertThat(getBearerTokenFilter(context)).isNull();
			});
	}

	@Test
	void autoConfigurationForJwtShouldBeConditionalOnJwtDecoderClass() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.withUserConfiguration(JwtDecoderConfig.class)
			.withClassLoader(new FilteredClassLoader(JwtDecoder.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(OAuth2ResourceServerAutoConfiguration.class);
				assertThat(getBearerTokenFilter(context)).isNull();
			});
	}

	@Test
	void jwtSecurityFilterShouldBeConditionalOnSecurityFilterChainClass() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.withUserConfiguration(JwtDecoderConfig.class)
			.withClassLoader(new FilteredClassLoader(SecurityFilterChain.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(OAuth2ResourceServerAutoConfiguration.class);
				assertThat(getBearerTokenFilter(context)).isNull();
			});
	}

	@Test
	void opaqueTokenSecurityFilterShouldBeConditionalOnSecurityFilterChainClass() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
					"spring.security.oauth2.resourceserver.opaquetoken.client-id=my-client-id",
					"spring.security.oauth2.resourceserver.opaquetoken.client-secret=my-client-secret")
			.withClassLoader(new FilteredClassLoader(SecurityFilterChain.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(OAuth2ResourceServerAutoConfiguration.class);
				assertThat(getBearerTokenFilter(context)).isNull();
			});
	}

	@Test
	void autoConfigurationWhenJwkSetUriAndIntrospectionUriAvailable() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
					"spring.security.oauth2.resourceserver.opaquetoken.client-id=my-client-id",
					"spring.security.oauth2.resourceserver.opaquetoken.client-secret=my-client-secret")
			.run((context) -> {
				assertThat(context).hasSingleBean(OpaqueTokenIntrospector.class);
				assertThat(context).hasSingleBean(JwtDecoder.class);
				assertThat(getBearerTokenFilter(context)).extracting("authenticationManagerResolver.arg$1.providers")
					.asInstanceOf(InstanceOfAssertFactories.LIST)
					.hasAtLeastOneElementOfType(JwtAuthenticationProvider.class);
			});
	}

	@Test
	void autoConfigurationWhenIntrospectionUriAvailableShouldConfigureIntrospectionClient() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
					"spring.security.oauth2.resourceserver.opaquetoken.client-id=my-client-id",
					"spring.security.oauth2.resourceserver.opaquetoken.client-secret=my-client-secret")
			.run((context) -> {
				assertThat(context).hasSingleBean(OpaqueTokenIntrospector.class);
				assertThat(getBearerTokenFilter(context)).isNotNull();
			});
	}

	@Test
	void opaqueTokenIntrospectorIsConditionalOnMissingBean() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com")
			.withUserConfiguration(OpaqueTokenIntrospectorConfig.class)
			.run((context) -> assertThat(getBearerTokenFilter(context)).isNotNull());
	}

	@Test
	void autoConfigurationWhenIntrospectionUriAvailableShouldBeConditionalOnClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(BearerTokenAuthenticationToken.class))
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
					"spring.security.oauth2.resourceserver.opaquetoken.client-id=my-client-id",
					"spring.security.oauth2.resourceserver.opaquetoken.client-secret=my-client-secret")
			.run((context) -> assertThat(context).doesNotHaveBean(OpaqueTokenIntrospector.class));
	}

	@Test
	void autoConfigurationShouldConfigureResourceServerUsingJwkSetUriAndIssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://" + this.server.getHostName() + ":"
							+ this.server.getPort() + "/" + path)
			.run((context) -> {
				assertThat(context).hasSingleBean(JwtDecoder.class);
				JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
				validate(jwt().claim("iss", issuer), jwtDecoder,
						(validators) -> assertThat(validators).hasAtLeastOneElementOfType(JwtIssuerValidator.class));
			});
	}

	@Test
	void autoConfigurationShouldNotConfigureIssuerUriAndAudienceJwtValidatorIfPropertyNotConfigured() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.run((context) -> {
				assertThat(context).hasSingleBean(JwtDecoder.class);
				JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
				validate(jwt(), jwtDecoder,
						(validators) -> assertThat(validators).hasSize(3).noneSatisfy(audClaimValidator()));
			});
	}

	@Test
	void autoConfigurationShouldConfigureAudienceAndIssuerJwtValidatorIfPropertyProvided() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		String issuerUri = "http://" + this.server.getHostName() + ":" + this.server.getPort() + "/" + path;
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
				"spring.security.oauth2.resourceserver.jwt.issuer-uri=" + issuerUri,
				"spring.security.oauth2.resourceserver.jwt.audiences=https://test-audience.com,https://test-audience1.com")
			.run((context) -> {
				assertThat(context).hasSingleBean(JwtDecoder.class);
				JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
				validate(
						jwt().claim("iss", URI.create(issuerUri).toURL())
							.claim("aud", List.of("https://test-audience.com")),
						jwtDecoder,
						(validators) -> assertThat(validators).hasAtLeastOneElementOfType(JwtIssuerValidator.class)
							.satisfiesOnlyOnce(audClaimValidator()));
			});
	}

	@SuppressWarnings("unchecked")
	@Test
	void autoConfigurationShouldConfigureAudienceValidatorIfPropertyProvidedAndIssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		String issuerUri = "http://" + this.server.getHostName() + ":" + this.server.getPort() + "/" + path;
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=" + issuerUri,
				"spring.security.oauth2.resourceserver.jwt.audiences=https://test-audience.com,https://test-audience1.com")
			.run((context) -> {
				SupplierJwtDecoder supplierJwtDecoderBean = context.getBean(SupplierJwtDecoder.class);
				Supplier<JwtDecoder> jwtDecoderSupplier = (Supplier<JwtDecoder>) ReflectionTestUtils
					.getField(supplierJwtDecoderBean, "delegate");
				assertThat(jwtDecoderSupplier).isNotNull();
				JwtDecoder jwtDecoder = jwtDecoderSupplier.get();
				validate(
						jwt().claim("iss", URI.create(issuerUri).toURL())
							.claim("aud", List.of("https://test-audience.com")),
						jwtDecoder,
						(validators) -> assertThat(validators).hasAtLeastOneElementOfType(JwtIssuerValidator.class)
							.satisfiesOnlyOnce(audClaimValidator()));
			});
	}

	@SuppressWarnings("unchecked")
	@Test
	void autoConfigurationShouldConfigureCustomValidators() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		String issuerUri = "http://" + this.server.getHostName() + ":" + this.server.getPort() + "/" + path;
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=" + issuerUri)
			.withUserConfiguration(CustomJwtClaimValidatorConfig.class)
			.run((context) -> {
				SupplierJwtDecoder supplierJwtDecoderBean = context.getBean(SupplierJwtDecoder.class);
				Supplier<JwtDecoder> jwtDecoderSupplier = (Supplier<JwtDecoder>) ReflectionTestUtils
					.getField(supplierJwtDecoderBean, "delegate");
				assertThat(jwtDecoderSupplier).isNotNull();
				JwtDecoder jwtDecoder = jwtDecoderSupplier.get();
				assertThat(context).hasBean("customJwtClaimValidator");
				OAuth2TokenValidator<Jwt> customValidator = (OAuth2TokenValidator<Jwt>) context
					.getBean("customJwtClaimValidator");
				validate(jwt().claim("iss", URI.create(issuerUri).toURL()).claim("custom_claim", "custom_claim_value"),
						jwtDecoder, (validators) -> assertThat(validators).contains(customValidator)
							.hasAtLeastOneElementOfType(JwtIssuerValidator.class));
			});
	}

	@Test
	@WithPublicKeyResource
	void autoConfigurationShouldConfigureAudienceValidatorIfPropertyProvidedAndPublicKey() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location",
				"spring.security.oauth2.resourceserver.jwt.audiences=https://test-audience.com,http://test-audience1.com")
			.run((context) -> {
				assertThat(context).hasSingleBean(JwtDecoder.class);
				JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
				validate(jwt().claim("aud", List.of("https://test-audience.com")), jwtDecoder,
						(validators) -> assertThat(validators).satisfiesOnlyOnce(audClaimValidator()));
			});
	}

	@SuppressWarnings("unchecked")
	@Test
	void audienceValidatorWhenAudienceInvalid() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		String issuerUri = "http://" + this.server.getHostName() + ":" + this.server.getPort() + "/" + path;
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
				"spring.security.oauth2.resourceserver.jwt.issuer-uri=" + issuerUri,
				"spring.security.oauth2.resourceserver.jwt.audiences=https://test-audience.com,https://test-audience1.com")
			.run((context) -> {
				assertThat(context).hasSingleBean(JwtDecoder.class);
				JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
				DelegatingOAuth2TokenValidator<Jwt> jwtValidator = (DelegatingOAuth2TokenValidator<Jwt>) ReflectionTestUtils
					.getField(jwtDecoder, "jwtValidator");
				assertThat(jwtValidator).isNotNull();
				Jwt jwt = jwt().claim("iss", new URL(issuerUri))
					.claim("aud", Collections.singletonList("https://other-audience.com"))
					.build();
				assertThat(jwtValidator.validate(jwt).hasErrors()).isTrue();
			});
	}

	@Test
	void jwtSecurityConfigurerBacksOffWhenSecurityFilterChainBeanIsPresent() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.withUserConfiguration(JwtDecoderConfig.class, TestSecurityFilterChainConfig.class)
			.run((context) -> assertThat(context).hasSingleBean(SecurityFilterChain.class));
	}

	@Test
	void opaqueTokenSecurityConfigurerBacksOffWhenSecurityFilterChainBeanIsPresent() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
			.withUserConfiguration(TestSecurityFilterChainConfig.class)
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
					"spring.security.oauth2.resourceserver.opaquetoken.client-id=my-client-id",
					"spring.security.oauth2.resourceserver.opaquetoken.client-secret=my-client-secret")
			.run((context) -> assertThat(context).hasSingleBean(SecurityFilterChain.class));
	}

	@ParameterizedTest(name = "{0}")
	@ArgumentsSource(JwtConverterCustomizationsArgumentsProvider.class)
	void autoConfigurationShouldConfigureResourceServerWithJwtConverterCustomizations(String[] properties, Jwt jwt,
			String expectedPrincipal, String[] expectedAuthorities) {
		this.contextRunner.withPropertyValues(properties).run((context) -> {
			JwtAuthenticationConverter converter = context.getBean(JwtAuthenticationConverter.class);
			AbstractAuthenticationToken token = converter.convert(jwt);
			assertThat(token).isNotNull().extracting(AbstractAuthenticationToken::getName).isEqualTo(expectedPrincipal);
			assertThat(token.getAuthorities()
				.stream()
				.filter((authority) -> !(authority instanceof FactorGrantedAuthority)))
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder(expectedAuthorities);
			assertThat(context).hasSingleBean(JwtDecoder.class);
			assertThat(getBearerTokenFilter(context)).isNotNull();
		});
	}

	@Test
	void shouldNotConfigureJwtConverterIfNoPropertiesAreSet() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(JwtAuthenticationConverter.class));
	}

	@Test
	void shouldConfigureJwtConverterIfPrincipalClaimNameIsSet() {
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.principal-claim-name=dummy")
			.run((context) -> assertThat(context).hasSingleBean(JwtAuthenticationConverter.class));
	}

	@Test
	void shouldConfigureJwtConverterIfAuthorityPrefixIsSet() {
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.authority-prefix=dummy")
			.run((context) -> assertThat(context).hasSingleBean(JwtAuthenticationConverter.class));
	}

	@Test
	void shouldConfigureJwtConverterIfAuthorityClaimsNameIsSet() {
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.authorities-claim-name=dummy")
			.run((context) -> assertThat(context).hasSingleBean(JwtAuthenticationConverter.class));
	}

	@Test
	void jwtAuthenticationConverterByJwtConfigIsConditionalOnMissingBean() {
		String propertiesPrincipalClaim = "principal_from_properties";
		String propertiesPrincipalValue = "from_props";
		String userConfigPrincipalValue = "from_user_config";
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.jwt.principal-claim-name=" + propertiesPrincipalClaim)
			.withUserConfiguration(CustomJwtConverterConfig.class)
			.run((context) -> {
				JwtAuthenticationConverter converter = context.getBean(JwtAuthenticationConverter.class);
				Jwt jwt = jwt().claim(propertiesPrincipalClaim, propertiesPrincipalValue)
					.claim(CustomJwtConverterConfig.PRINCIPAL_CLAIM, userConfigPrincipalValue)
					.build();
				AbstractAuthenticationToken token = converter.convert(jwt);
				assertThat(token).isNotNull()
					.extracting(AbstractAuthenticationToken::getName)
					.isEqualTo(userConfigPrincipalValue)
					.isNotEqualTo(propertiesPrincipalValue);
				assertThat(context).hasSingleBean(JwtDecoder.class);
				assertThat(getBearerTokenFilter(context)).isNotNull();
			});
	}

	@Test
	void causesManagementWebSecurityAutoConfigurationToBackOff() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ManagementWebSecurityAutoConfiguration.class,
					OAuth2ResourceServerAutoConfiguration.class, SecurityAutoConfiguration.class,
					ServletWebSecurityAutoConfiguration.class, WebMvcAutoConfiguration.class));
		contextRunner.run((context) -> assertThat(context).hasSingleBean(ManagementWebSecurityAutoConfiguration.class));
		contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://authserver")
			.run((context) -> assertThat(context).doesNotHaveBean(ManagementWebSecurityAutoConfiguration.class)
				.doesNotHaveBean(MANAGEMENT_SECURITY_FILTER_CHAIN_BEAN));
	}

	private @Nullable Filter getBearerTokenFilter(AssertableWebApplicationContext context) {
		FilterChainProxy filterChain = (FilterChainProxy) context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<SecurityFilterChain> filterChains = filterChain.getFilterChains();
		List<Filter> filters = filterChains.get(0).getFilters();
		return filters.stream().filter((f) -> f instanceof BearerTokenAuthenticationFilter).findFirst().orElse(null);
	}

	private String cleanIssuerPath(String issuer) {
		if (issuer.endsWith("/")) {
			return issuer.substring(0, issuer.length() - 1);
		}
		return issuer;
	}

	private void setupMockResponse(String issuer) {
		MockResponse mockResponse = new MockResponse().setResponseCode(HttpStatus.OK.value())
			.setBody(new JsonMapper().writeValueAsString(getResponse(issuer)))
			.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		assertThat(this.server).isNotNull();
		this.server.enqueue(mockResponse);
		this.server.enqueue(
				new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(JWK_SET));
	}

	private void setupMockResponsesWithErrors(String issuer, int errorResponseCount) {
		assertThat(this.server).isNotNull();
		for (int i = 0; i < errorResponseCount; i++) {
			MockResponse emptyResponse = new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value());
			this.server.enqueue(emptyResponse);
		}
		setupMockResponse(issuer);
	}

	private Map<String, Object> getResponse(String issuer) {
		Map<String, Object> response = new HashMap<>();
		response.put("authorization_endpoint", "https://example.com/o/oauth2/v2/auth");
		response.put("claims_supported", Collections.emptyList());
		response.put("code_challenge_methods_supported", Collections.emptyList());
		response.put("id_token_signing_alg_values_supported", Collections.emptyList());
		response.put("issuer", issuer);
		response.put("jwks_uri", issuer + "/.well-known/jwks.json");
		response.put("response_types_supported", Collections.emptyList());
		response.put("revocation_endpoint", "https://example.com/o/oauth2/revoke");
		response.put("scopes_supported", Collections.singletonList("openid"));
		response.put("subject_types_supported", Collections.singletonList("public"));
		response.put("grant_types_supported", Collections.singletonList("authorization_code"));
		response.put("token_endpoint", "https://example.com/oauth2/v4/token");
		response.put("token_endpoint_auth_methods_supported", Collections.singletonList("client_secret_basic"));
		response.put("userinfo_endpoint", "https://example.com/oauth2/v3/userinfo");
		return response;
	}

	static Jwt.Builder jwt() {
		return Jwt.withTokenValue("token")
			.header("alg", "none")
			.expiresAt(Instant.MAX)
			.issuedAt(Instant.MIN)
			.issuer("https://issuer.example.org")
			.jti("jti")
			.notBefore(Instant.MIN)
			.subject("mock-test-subject");
	}

	@SuppressWarnings("unchecked")
	private void validate(Jwt.Builder builder, JwtDecoder jwtDecoder,
			ThrowingConsumer<List<OAuth2TokenValidator<Jwt>>> validatorsConsumer) {
		DelegatingOAuth2TokenValidator<Jwt> jwtValidator = (DelegatingOAuth2TokenValidator<Jwt>) ReflectionTestUtils
			.getField(jwtDecoder, "jwtValidator");
		assertThat(jwtValidator).isNotNull();
		assertThat(jwtValidator.validate(builder.build()).hasErrors()).isFalse();
		validatorsConsumer.accept(extractValidators(jwtValidator));
	}

	@SuppressWarnings("unchecked")
	private List<OAuth2TokenValidator<Jwt>> extractValidators(DelegatingOAuth2TokenValidator<Jwt> delegatingValidator) {
		Collection<OAuth2TokenValidator<Jwt>> delegates = (Collection<OAuth2TokenValidator<Jwt>>) ReflectionTestUtils
			.getField(delegatingValidator, "tokenValidators");
		assertThat(delegates).isNotNull();
		List<OAuth2TokenValidator<Jwt>> extracted = new ArrayList<>();
		for (OAuth2TokenValidator<Jwt> delegate : delegates) {
			if (delegate instanceof DelegatingOAuth2TokenValidator<Jwt> delegatingDelegate) {
				extracted.addAll(extractValidators(delegatingDelegate));
			}
			else {
				extracted.add(delegate);
			}
		}
		return extracted;
	}

	private Consumer<OAuth2TokenValidator<Jwt>> audClaimValidator() {
		return (validator) -> assertThat(validator).isInstanceOf(JwtClaimValidator.class)
			.extracting("claim")
			.isEqualTo("aud");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	static class TestConfig {

		@Bean
		@Order(1)
		JwkSetUriJwtDecoderBuilderCustomizer decoderBuilderCustomizer() {
			return mock(JwkSetUriJwtDecoderBuilderCustomizer.class);
		}

		@Bean
		@Order(2)
		JwkSetUriJwtDecoderBuilderCustomizer anotherDecoderBuilderCustomizer() {
			return mock(JwkSetUriJwtDecoderBuilderCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	static class JwtDecoderConfig {

		@Bean
		JwtDecoder decoder() {
			return mock(JwtDecoder.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	static class OpaqueTokenIntrospectorConfig {

		@Bean
		OpaqueTokenIntrospector decoder() {
			return mock(OpaqueTokenIntrospector.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	static class TestSecurityFilterChainConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(HttpSecurity http) {
			http.securityMatcher("/**");
			http.authorizeHttpRequests((requests) -> requests.anyRequest().authenticated());
			return http.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJwtClaimValidatorConfig {

		@Bean
		JwtClaimValidator<String> customJwtClaimValidator() {
			return new JwtClaimValidator<>("custom_claim", "custom_claim_value"::equals);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJwtConverterConfig {

		static String PRINCIPAL_CLAIM = "principal_from_user_configuration";

		@Bean
		JwtAuthenticationConverter customJwtAuthenticationConverter() {
			JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
			converter.setPrincipalClaimName(PRINCIPAL_CLAIM);
			return converter;
		}

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "public-key-location", content = """
			-----BEGIN PUBLIC KEY-----
			MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDdlatRjRjogo3WojgGHFHYLugd
			UWAY9iR3fy4arWNA1KoS8kVw33cJibXr8bvwUAUparCwlvdbH6dvEOfou0/gCFQs
			HUfQrSDv+MuSUMAe8jzKE4qW+jK+xQU9a03GUnKHkkle+Q0pX/g6jXZ7r1/xAK5D
			o2kQ+X5xK9cipRgEKwIDAQAB
			-----END PUBLIC KEY-----
			""")
	@interface WithPublicKeyResource {

	}

}
