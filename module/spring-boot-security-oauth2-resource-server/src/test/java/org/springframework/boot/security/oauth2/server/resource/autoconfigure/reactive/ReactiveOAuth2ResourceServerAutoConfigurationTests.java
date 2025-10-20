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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.nimbusds.jose.JWSAlgorithm;
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
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.JwtConverterCustomizationsArgumentsProvider;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.ReactiveAuthenticationManagerResolver;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.MatcherSecurityWebFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Tests for {@link ReactiveOAuth2ResourceServerAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Anastasiia Losieva
 * @author Mushtaq Ahmed
 * @author Roman Golovin
 * @author Yan Kardziyaka
 */
class ReactiveOAuth2ResourceServerAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ReactiveOAuth2ResourceServerAutoConfiguration.class))
		.withUserConfiguration(TestConfig.class);

	private @Nullable MockWebServer server;

	private static final Duration TIMEOUT = Duration.ofSeconds(5000000);

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
				assertThat(context).hasSingleBean(NimbusReactiveJwtDecoder.class);
				assertFilterConfiguredWithJwtAuthenticationManager(context);
			});
	}

	@Test
	void autoConfigurationUsingJwkSetUriShouldConfigureResourceServerUsingSingleJwsAlgorithm() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.jwt.jws-algorithms=RS512")
			.run((context) -> {
				NimbusReactiveJwtDecoder nimbusReactiveJwtDecoder = context.getBean(NimbusReactiveJwtDecoder.class);
				assertThat(nimbusReactiveJwtDecoder).extracting("jwtProcessor.arg$1.signatureAlgorithms")
					.asInstanceOf(InstanceOfAssertFactories.collection(SignatureAlgorithm.class))
					.containsExactlyInAnyOrder(SignatureAlgorithm.RS512);
				assertJwkSetUriReactiveJwtDecoderBuilderCustomization(context);
			});
	}

	private void assertJwkSetUriReactiveJwtDecoderBuilderCustomization(
			AssertableReactiveWebApplicationContext context) {
		JwkSetUriReactiveJwtDecoderBuilderCustomizer customizer = context.getBean("decoderBuilderCustomizer",
				JwkSetUriReactiveJwtDecoderBuilderCustomizer.class);
		JwkSetUriReactiveJwtDecoderBuilderCustomizer anotherCustomizer = context
			.getBean("anotherDecoderBuilderCustomizer", JwkSetUriReactiveJwtDecoderBuilderCustomizer.class);
		InOrder inOrder = inOrder(customizer, anotherCustomizer);
		inOrder.verify(customizer).customize(any());
		inOrder.verify(anotherCustomizer).customize(any());
	}

	@Test
	void autoConfigurationUsingJwkSetUriShouldConfigureResourceServerUsingMultipleJwsAlgorithms() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.jwt.jws-algorithms=RS256, RS384, RS512")
			.run((context) -> {
				NimbusReactiveJwtDecoder nimbusReactiveJwtDecoder = context.getBean(NimbusReactiveJwtDecoder.class);
				assertThat(nimbusReactiveJwtDecoder).extracting("jwtProcessor.arg$1.signatureAlgorithms")
					.asInstanceOf(InstanceOfAssertFactories.collection(SignatureAlgorithm.class))
					.containsExactlyInAnyOrder(SignatureAlgorithm.RS256, SignatureAlgorithm.RS384,
							SignatureAlgorithm.RS512);
				assertJwkSetUriReactiveJwtDecoderBuilderCustomization(context);
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
				NimbusReactiveJwtDecoder nimbusReactiveJwtDecoder = context.getBean(NimbusReactiveJwtDecoder.class);
				assertThat(nimbusReactiveJwtDecoder).extracting("jwtProcessor.arg$1.jwsKeySelector.expectedJWSAlg")
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

	@Test
	void autoConfigurationShouldConfigureResourceServerUsingOidcIssuerUri() throws IOException {
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
				assertThat(context).hasSingleBean(SupplierReactiveJwtDecoder.class);
				assertFilterConfiguredWithJwtAuthenticationManager(context);
				assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
				// Trigger calls to the issuer by decoding a token
				decodeJwt(context);
				assertJwkSetUriReactiveJwtDecoderBuilderCustomization(context);
			});
		// The last request is to the JWK Set endpoint to look up the algorithm
		assertThat(this.server.getRequestCount()).isEqualTo(2);
	}

	@SuppressWarnings("unchecked")
	private void decodeJwt(AssertableReactiveWebApplicationContext context) {
		SupplierReactiveJwtDecoder supplierReactiveJwtDecoder = context.getBean(SupplierReactiveJwtDecoder.class);
		Mono<ReactiveJwtDecoder> reactiveJwtDecoderSupplier = (Mono<ReactiveJwtDecoder>) ReflectionTestUtils
			.getField(supplierReactiveJwtDecoder, "jwtDecoderMono");
		assertThat(reactiveJwtDecoderSupplier).isNotNull();
		try {
			reactiveJwtDecoderSupplier.flatMap((decoder) -> decoder.decode("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9."
					+ "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0."
					+ "NHVaYe26MbtOYhSKkoKYdFVomg4i8ZJd8_-RU8VNbftc4TSMb4bXP3l3YlNWACwyXPGffz5aXHc6lty1Y2t4SWRqGteragsVdZufDn5BlnJl9pdR_kdVFUsra2rWKEofkZeIC4yWytE58sMIihvo9H1ScmmVwBcQP6XETqYd0aSHp1gOa9RdUPDvoXQ5oqygTqVtxaDr6wUFKrKItgBMzWIdNZ6y7O9E0DhEPTbE9rfBo6KTFsHAZnMg4k68CDp2woYIaXbmYTWcvbzIuHO7_37GT79XdIwkm95QJ7hYC9RiwrV7mesbY4PAahERJawntho0my942XheVLmGwLMBkQ"))
				.block(TIMEOUT);
		}
		catch (Exception ex) {
			// This fails, but it's enough to check that the expected HTTP calls
			// are made
		}
	}

	@Test
	void autoConfigurationShouldConfigureResourceServerUsingOidcRfc8414IssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponsesWithErrors(cleanIssuerPath, 1);
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://"
					+ this.server.getHostName() + ":" + this.server.getPort())
			.run((context) -> {
				assertThat(context).hasSingleBean(SupplierReactiveJwtDecoder.class);
				assertFilterConfiguredWithJwtAuthenticationManager(context);
				assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
				// Trigger calls to the issuer by decoding a token
				decodeJwt(context);
				// assertJwkSetUriReactiveJwtDecoderBuilderCustomization(context);
			});
		// The last request is to the JWK Set endpoint to look up the algorithm
		assertThat(this.server.getRequestCount()).isEqualTo(3);
	}

	@Test
	void autoConfigurationShouldConfigureResourceServerUsingOAuthIssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponsesWithErrors(cleanIssuerPath, 2);
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://"
					+ this.server.getHostName() + ":" + this.server.getPort())
			.run((context) -> {
				assertThat(context).hasSingleBean(SupplierReactiveJwtDecoder.class);
				assertFilterConfiguredWithJwtAuthenticationManager(context);
				assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
				// Trigger calls to the issuer by decoding a token
				decodeJwt(context);
				assertJwkSetUriReactiveJwtDecoderBuilderCustomization(context);
			});
		// The last request is to the JWK Set endpoint to look up the algorithm
		assertThat(this.server.getRequestCount()).isEqualTo(4);
	}

	@Test
	@WithPublicKeyResource
	void autoConfigurationShouldConfigureResourceServerUsingPublicKeyValue() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location")
			.run((context) -> {
				assertThat(context).hasSingleBean(NimbusReactiveJwtDecoder.class);
				assertFilterConfiguredWithJwtAuthenticationManager(context);
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
	void autoConfigurationWhenSetUriKeyLocationIssuerUriPresentShouldUseSetUri() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location",
					"spring.security.oauth2.resourceserver.jwt.issuer-uri=https://jwk-oidc-issuer-location.com")
			.run((context) -> {
				assertThat(context).hasSingleBean(NimbusReactiveJwtDecoder.class);
				assertFilterConfiguredWithJwtAuthenticationManager(context);
				assertThat(context.containsBean("jwtDecoder")).isTrue();
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
				assertThat(context).hasSingleBean(SupplierReactiveJwtDecoder.class);
				assertFilterConfiguredWithJwtAuthenticationManager(context);
				assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
			});
	}

	@Test
	void autoConfigurationWhenJwkSetUriNullShouldNotFail() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN));
	}

	@Test
	void jwtDecoderBeanIsConditionalOnMissingBean() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.withUserConfiguration(JwtDecoderConfig.class)
			.run((this::assertFilterConfiguredWithJwtAuthenticationManager));
	}

	@Test
	void jwtDecoderByIssuerUriBeanIsConditionalOnMissingBean() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.jwt.issuer-uri=https://jwk-oidc-issuer-location.com")
			.withUserConfiguration(JwtDecoderConfig.class)
			.run((this::assertFilterConfiguredWithJwtAuthenticationManager));
	}

	@Test
	void autoConfigurationShouldBeConditionalOnBearerTokenAuthenticationTokenClass() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.withUserConfiguration(JwtDecoderConfig.class)
			.withClassLoader(new FilteredClassLoader(BearerTokenAuthenticationToken.class))
			.run((context) -> assertThat(context).doesNotHaveBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN));
	}

	@Test
	void autoConfigurationShouldBeConditionalOnReactiveJwtDecoderClass() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.withUserConfiguration(JwtDecoderConfig.class)
			.withClassLoader(new FilteredClassLoader(ReactiveJwtDecoder.class))
			.run((context) -> assertThat(context).doesNotHaveBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN));
	}

	@Test
	void autoConfigurationWhenSecurityWebFilterChainConfigPresentShouldNotAddOne() {
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
			.withUserConfiguration(SecurityWebFilterChainConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
				assertThat(context).hasBean("testSpringSecurityFilterChain");
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
				assertThat(context).hasSingleBean(ReactiveOpaqueTokenIntrospector.class);
				assertFilterConfiguredWithOpaqueTokenAuthenticationManager(context);
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
				assertThat(context).hasSingleBean(ReactiveOpaqueTokenIntrospector.class);
				assertThat(context).hasSingleBean(ReactiveJwtDecoder.class);
				assertFilterConfiguredWithJwtAuthenticationManager(context);
			});
	}

	@Test
	void opaqueTokenIntrospectorIsConditionalOnMissingBean() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com")
			.withUserConfiguration(OpaqueTokenIntrospectorConfig.class)
			.run((this::assertFilterConfiguredWithOpaqueTokenAuthenticationManager));
	}

	@Test
	void autoConfigurationForOpaqueTokenWhenSecurityWebFilterChainConfigPresentShouldNotAddOne() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
					"spring.security.oauth2.resourceserver.opaquetoken.client-id=my-client-id",
					"spring.security.oauth2.resourceserver.opaquetoken.client-secret=my-client-secret")
			.withUserConfiguration(SecurityWebFilterChainConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
				assertThat(context).hasBean("testSpringSecurityFilterChain");
			});
	}

	@Test
	void autoConfigurationWhenIntrospectionUriAvailableShouldBeConditionalOnClass() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(BearerTokenAuthenticationToken.class))
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
					"spring.security.oauth2.resourceserver.opaquetoken.client-id=my-client-id",
					"spring.security.oauth2.resourceserver.opaquetoken.client-secret=my-client-secret")
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveOpaqueTokenIntrospector.class));
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
				assertThat(context).hasSingleBean(ReactiveJwtDecoder.class);
				ReactiveJwtDecoder reactiveJwtDecoder = context.getBean(ReactiveJwtDecoder.class);
				validate(jwt().claim("iss", issuer), reactiveJwtDecoder,
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
				assertThat(context).hasSingleBean(ReactiveJwtDecoder.class);
				ReactiveJwtDecoder reactiveJwtDecoder = context.getBean(ReactiveJwtDecoder.class);
				validate(jwt(), reactiveJwtDecoder,
						(validators) -> assertThat(validators).hasSize(3).noneSatisfy(audClaimValidator()));
			});
	}

	@Test
	void autoConfigurationShouldConfigureIssuerAndAudienceJwtValidatorIfPropertyProvided() throws Exception {
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
				assertThat(context).hasSingleBean(ReactiveJwtDecoder.class);
				ReactiveJwtDecoder reactiveJwtDecoder = context.getBean(ReactiveJwtDecoder.class);
				validate(
						jwt().claim("iss", URI.create(issuerUri).toURL())
							.claim("aud", List.of("https://test-audience.com")),
						reactiveJwtDecoder,
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
				SupplierReactiveJwtDecoder supplierJwtDecoderBean = context.getBean(SupplierReactiveJwtDecoder.class);
				Mono<ReactiveJwtDecoder> jwtDecoderSupplier = (Mono<ReactiveJwtDecoder>) ReflectionTestUtils
					.getField(supplierJwtDecoderBean, "jwtDecoderMono");
				assertThat(jwtDecoderSupplier).isNotNull();
				ReactiveJwtDecoder jwtDecoder = jwtDecoderSupplier.block();
				assertThat(jwtDecoder).isNotNull();
				validate(
						jwt().claim("iss", URI.create(issuerUri).toURL())
							.claim("aud", List.of("https://test-audience.com")),
						jwtDecoder,
						(validators) -> assertThat(validators).hasAtLeastOneElementOfType(JwtIssuerValidator.class)
							.satisfiesOnlyOnce(audClaimValidator()));
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
				"spring.security.oauth2.resourceserver.jwt.audiences=https://test-audience.com,https://test-audience1.com")
			.run((context) -> {
				assertThat(context).hasSingleBean(ReactiveJwtDecoder.class);
				ReactiveJwtDecoder jwtDecoder = context.getBean(ReactiveJwtDecoder.class);
				validate(jwt().claim("aud", List.of("https://test-audience.com")), jwtDecoder,
						(validators) -> assertThat(validators).satisfiesOnlyOnce(audClaimValidator()));
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
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.jwt.issuer-uri=" + issuerUri)
			.withUserConfiguration(CustomJwtClaimValidatorConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ReactiveJwtDecoder.class);
				ReactiveJwtDecoder reactiveJwtDecoder = context.getBean(ReactiveJwtDecoder.class);
				OAuth2TokenValidator<Jwt> customValidator = (OAuth2TokenValidator<Jwt>) context
					.getBean("customJwtClaimValidator");
				validate(jwt().claim("iss", URI.create(issuerUri).toURL()).claim("custom_claim", "custom_claim_value"),
						reactiveJwtDecoder, (validators) -> assertThat(validators).contains(customValidator)
							.hasAtLeastOneElementOfType(JwtIssuerValidator.class));
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
				assertThat(context).hasSingleBean(ReactiveJwtDecoder.class);
				ReactiveJwtDecoder jwtDecoder = context.getBean(ReactiveJwtDecoder.class);
				DelegatingOAuth2TokenValidator<Jwt> jwtValidator = (DelegatingOAuth2TokenValidator<Jwt>) ReflectionTestUtils
					.getField(jwtDecoder, "jwtValidator");
				assertThat(jwtValidator).isNotNull();
				Jwt jwt = jwt().claim("iss", new URL(issuerUri))
					.claim("aud", Collections.singletonList("https://other-audience.com"))
					.build();
				assertThat(jwtValidator.validate(jwt).hasErrors()).isTrue();
			});
	}

	@SuppressWarnings("unchecked")
	@Test
	void customValidatorWhenInvalid() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String path = "test";
		String issuer = this.server.url(path).toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		String issuerUri = "http://" + this.server.getHostName() + ":" + this.server.getPort() + "/" + path;
		this.contextRunner
			.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
					"spring.security.oauth2.resourceserver.jwt.issuer-uri=" + issuerUri)
			.withUserConfiguration(CustomJwtClaimValidatorConfig.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ReactiveJwtDecoder.class);
				ReactiveJwtDecoder jwtDecoder = context.getBean(ReactiveJwtDecoder.class);
				DelegatingOAuth2TokenValidator<Jwt> jwtValidator = (DelegatingOAuth2TokenValidator<Jwt>) ReflectionTestUtils
					.getField(jwtDecoder, "jwtValidator");
				assertThat(jwtValidator).isNotNull();
				Jwt jwt = jwt().claim("iss", new URL(issuerUri)).claim("custom_claim", "invalid_value").build();
				assertThat(jwtValidator.validate(jwt).hasErrors()).isTrue();
			});
	}

	@Test
	void shouldNotConfigureJwtConverterIfNoPropertiesAreSet() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveJwtAuthenticationConverter.class));
	}

	@Test
	void shouldConfigureJwtConverterIfPrincipalClaimNameIsSet() {
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.principal-claim-name=dummy")
			.run((context) -> assertThat(context).hasSingleBean(ReactiveJwtAuthenticationConverter.class));
	}

	@Test
	void shouldConfigureJwtConverterIfAuthorityPrefixIsSet() {
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.authority-prefix=dummy")
			.run((context) -> assertThat(context).hasSingleBean(ReactiveJwtAuthenticationConverter.class));
	}

	@Test
	void shouldConfigureJwtConverterIfAuthorityClaimsNameIsSet() {
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.authorities-claim-name=dummy")
			.run((context) -> assertThat(context).hasSingleBean(ReactiveJwtAuthenticationConverter.class));
	}

	@ParameterizedTest(name = "{0}")
	@ArgumentsSource(JwtConverterCustomizationsArgumentsProvider.class)
	void autoConfigurationShouldConfigureResourceServerWithJwtConverterCustomizations(String[] properties, Jwt jwt,
			String expectedPrincipal, String[] expectedAuthorities) {
		this.contextRunner.withPropertyValues(properties).run((context) -> {
			ReactiveJwtAuthenticationConverter converter = context.getBean(ReactiveJwtAuthenticationConverter.class);
			AbstractAuthenticationToken token = converter.convert(jwt).block();
			assertThat(token).isNotNull().extracting(AbstractAuthenticationToken::getName).isEqualTo(expectedPrincipal);
			assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder(expectedAuthorities);
			assertThat(context).hasSingleBean(NimbusReactiveJwtDecoder.class);
			assertFilterConfiguredWithJwtAuthenticationManager(context);
		});
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
				ReactiveJwtAuthenticationConverter converter = context
					.getBean(ReactiveJwtAuthenticationConverter.class);
				Jwt jwt = jwt().claim(propertiesPrincipalClaim, propertiesPrincipalValue)
					.claim(CustomJwtConverterConfig.PRINCIPAL_CLAIM, userConfigPrincipalValue)
					.build();
				AbstractAuthenticationToken token = converter.convert(jwt).block();
				assertThat(token).isNotNull()
					.extracting(AbstractAuthenticationToken::getName)
					.isEqualTo(userConfigPrincipalValue)
					.isNotEqualTo(propertiesPrincipalValue);
				assertThat(context).hasSingleBean(NimbusReactiveJwtDecoder.class);
				assertFilterConfiguredWithJwtAuthenticationManager(context);
			});
	}

	@Test
	void causesReactiveManagementWebSecurityAutoConfigurationToBackOff() {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveManagementWebSecurityAutoConfiguration.class,
					ReactiveOAuth2ResourceServerAutoConfiguration.class, ReactiveWebSecurityAutoConfiguration.class,
					WebFluxAutoConfiguration.class));
		contextRunner
			.run((context) -> assertThat(context).hasSingleBean(ReactiveManagementWebSecurityAutoConfiguration.class));
		contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://authserver")
			.run((context) -> assertThat(context)
				.doesNotHaveBean(ReactiveManagementWebSecurityAutoConfiguration.class));
	}

	@SuppressWarnings("unchecked")
	private void assertFilterConfiguredWithJwtAuthenticationManager(AssertableReactiveWebApplicationContext context) {
		MatcherSecurityWebFilterChain filterChain = (MatcherSecurityWebFilterChain) context
			.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		Stream<WebFilter> filters = filterChain.getWebFilters().toStream();
		AuthenticationWebFilter webFilter = (AuthenticationWebFilter) filters
			.filter((f) -> f instanceof AuthenticationWebFilter)
			.findFirst()
			.orElse(null);
		assertThat(webFilter).isNotNull();
		ReactiveAuthenticationManagerResolver<ServerWebExchange> authenticationManagerResolver = (ReactiveAuthenticationManagerResolver<ServerWebExchange>) ReflectionTestUtils
			.getField(webFilter, "authenticationManagerResolver");
		assertThat(authenticationManagerResolver).isNotNull();
		Object authenticationManager = authenticationManagerResolver.resolve(mock(ServerWebExchange.class))
			.block(TIMEOUT);
		assertThat(authenticationManager).isInstanceOf(JwtReactiveAuthenticationManager.class);
	}

	@SuppressWarnings("unchecked")
	private void assertFilterConfiguredWithOpaqueTokenAuthenticationManager(
			AssertableReactiveWebApplicationContext context) {
		MatcherSecurityWebFilterChain filterChain = (MatcherSecurityWebFilterChain) context
			.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		Stream<WebFilter> filters = filterChain.getWebFilters().toStream();
		AuthenticationWebFilter webFilter = (AuthenticationWebFilter) filters
			.filter((f) -> f instanceof AuthenticationWebFilter)
			.findFirst()
			.orElse(null);
		assertThat(webFilter).isNotNull();
		ReactiveAuthenticationManagerResolver<ServerWebExchange> authenticationManagerResolver = (ReactiveAuthenticationManagerResolver<ServerWebExchange>) ReflectionTestUtils
			.getField(webFilter, "authenticationManagerResolver");
		assertThat(authenticationManagerResolver).isNotNull();
		Object authenticationManager = authenticationManagerResolver.resolve(mock(ServerWebExchange.class))
			.block(TIMEOUT);
		assertThat(authenticationManager).isInstanceOf(OpaqueTokenReactiveAuthenticationManager.class);
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
	private void validate(Jwt.Builder builder, ReactiveJwtDecoder jwtDecoder,
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

	@EnableWebFluxSecurity
	static class TestConfig {

		@Bean
		MapReactiveUserDetailsService userDetailsService() {
			return mock(MapReactiveUserDetailsService.class);
		}

		@Bean
		@Order(1)
		JwkSetUriReactiveJwtDecoderBuilderCustomizer decoderBuilderCustomizer() {
			return mock(JwkSetUriReactiveJwtDecoderBuilderCustomizer.class);
		}

		@Bean
		@Order(2)
		JwkSetUriReactiveJwtDecoderBuilderCustomizer anotherDecoderBuilderCustomizer() {
			return mock(JwkSetUriReactiveJwtDecoderBuilderCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JwtDecoderConfig {

		@Bean
		ReactiveJwtDecoder decoder() {
			return mock(ReactiveJwtDecoder.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OpaqueTokenIntrospectorConfig {

		@Bean
		ReactiveOpaqueTokenIntrospector decoder() {
			return mock(ReactiveOpaqueTokenIntrospector.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityWebFilterChainConfig {

		@Bean
		SecurityWebFilterChain testSpringSecurityFilterChain(ServerHttpSecurity http) {
			http.authorizeExchange((exchanges) -> {
				exchanges.pathMatchers("/message/**").hasRole("ADMIN");
				exchanges.anyExchange().authenticated();
			});
			http.httpBasic(withDefaults());
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
		ReactiveJwtAuthenticationConverter customReactiveJwtAuthenticationConverter() {
			ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
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
