/*
 * Copyright 2012-2020 the original author or authors.
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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.ReactiveAuthenticationManagerResolver;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.MatcherSecurityWebFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveOAuth2ResourceServerAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 * @author HaiTao Zhang
 * @author Anastasiia Losieva
 */
class ReactiveOAuth2ResourceServerAutoConfigurationTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveOAuth2ResourceServerAutoConfiguration.class))
			.withUserConfiguration(TestConfig.class);

	private MockWebServer server;

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

	@SuppressWarnings("unchecked")
	@Test
	void autoConfigurationUsingJwkSetUriShouldConfigureResourceServerUsingJwsAlgorithm() {
		this.contextRunner
				.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com",
						"spring.security.oauth2.resourceserver.jwt.jws-algorithm=RS512")
				.run((context) -> {
					NimbusReactiveJwtDecoder nimbusReactiveJwtDecoder = context.getBean(NimbusReactiveJwtDecoder.class);
					assertThat(nimbusReactiveJwtDecoder).extracting("jwtProcessor.arg$2")
							.matches((algorithms) -> ((Set<JWSAlgorithm>) algorithms).contains(JWSAlgorithm.RS512));
				});
	}

	@Test
	void autoConfigurationUsingPublicKeyValueShouldConfigureResourceServerUsingJwsAlgorithm() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location",
				"spring.security.oauth2.resourceserver.jwt.jws-algorithm=RS384").run((context) -> {
					NimbusReactiveJwtDecoder nimbusReactiveJwtDecoder = context.getBean(NimbusReactiveJwtDecoder.class);
					assertThat(nimbusReactiveJwtDecoder)
							.extracting("jwtProcessor.arg$1.jwsKeySelector.expectedJwsAlgorithm")
							.isEqualTo(JWSAlgorithm.RS384);
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
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://"
				+ this.server.getHostName() + ":" + this.server.getPort() + "/" + path).run((context) -> {
					assertThat(context).hasSingleBean(NimbusReactiveJwtDecoder.class);
					assertFilterConfiguredWithJwtAuthenticationManager(context);
					assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
				});
		assertThat(this.server.getRequestCount()).isEqualTo(1);
	}

	@Test
	void autoConfigurationShouldConfigureResourceServerUsingOidcRfc8414IssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponsesWithErrors(cleanIssuerPath, 1);
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://"
				+ this.server.getHostName() + ":" + this.server.getPort()).run((context) -> {
					assertThat(context).hasSingleBean(NimbusReactiveJwtDecoder.class);
					assertFilterConfiguredWithJwtAuthenticationManager(context);
					assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
				});
		assertThat(this.server.getRequestCount()).isEqualTo(2);
	}

	@Test
	void autoConfigurationShouldConfigureResourceServerUsingOAuthIssuerUri() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponsesWithErrors(cleanIssuerPath, 2);
		this.contextRunner.withPropertyValues("spring.security.oauth2.resourceserver.jwt.issuer-uri=http://"
				+ this.server.getHostName() + ":" + this.server.getPort()).run((context) -> {
					assertThat(context).hasSingleBean(NimbusReactiveJwtDecoder.class);
					assertFilterConfiguredWithJwtAuthenticationManager(context);
					assertThat(context.containsBean("jwtDecoderByIssuerUri")).isTrue();
				});
		assertThat(this.server.getRequestCount()).isEqualTo(3);
	}

	@Test
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
				.run((context) -> assertThat(context).hasFailed().getFailure()
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
					assertThat(context).hasSingleBean(NimbusReactiveJwtDecoder.class);
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
				.withUserConfiguration(SecurityWebFilterChainConfig.class).run((context) -> {
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
				.withUserConfiguration(SecurityWebFilterChainConfig.class).run((context) -> {
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
	void autoConfigurationWhenBothJwkSetUriAndTokenIntrospectionUriSetShouldFail() {
		this.contextRunner
				.withPropertyValues(
						"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
						"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://jwk-set-uri.com")
				.run((context) -> assertThat(context).hasFailed().getFailure().hasMessageContaining(
						"Only one of jwt.jwk-set-uri and opaquetoken.introspection-uri should be configured."));
	}

	@Test
	void autoConfigurationWhenBothJwtIssuerUriAndTokenIntrospectionUriSetShouldFail() {
		this.contextRunner
				.withPropertyValues(
						"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
						"spring.security.oauth2.resourceserver.jwt.issuer-uri=https://jwk-oidc-issuer-location.com")
				.run((context) -> assertThat(context).hasFailed().getFailure().hasMessageContaining(
						"Only one of jwt.issuer-uri and opaquetoken.introspection-uri should be configured."));
	}

	@Test
	void autoConfigurationWhenBothJwtKeyLocationAndTokenIntrospectionUriSetShouldFail() {
		this.contextRunner
				.withPropertyValues(
						"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://check-token.com",
						"spring.security.oauth2.resourceserver.jwt.public-key-location=classpath:public-key-location")
				.run((context) -> assertThat(context).hasFailed().getFailure().hasMessageContaining(
						"Only one of jwt.public-key-location and opaquetoken.introspection-uri should be configured."));
	}

	@SuppressWarnings("unchecked")
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
					DelegatingOAuth2TokenValidator<Jwt> jwtValidator = (DelegatingOAuth2TokenValidator) ReflectionTestUtils
							.getField(reactiveJwtDecoder, "jwtValidator");
					Collection<OAuth2TokenValidator<Jwt>> tokenValidators = (Collection<OAuth2TokenValidator<Jwt>>) ReflectionTestUtils
							.getField(jwtValidator, "tokenValidators");
					assertThat(tokenValidators).hasAtLeastOneElementOfType(JwtIssuerValidator.class);
				});
	}

	private void assertFilterConfiguredWithJwtAuthenticationManager(AssertableReactiveWebApplicationContext context) {
		MatcherSecurityWebFilterChain filterChain = (MatcherSecurityWebFilterChain) context
				.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		Stream<WebFilter> filters = filterChain.getWebFilters().toStream();
		AuthenticationWebFilter webFilter = (AuthenticationWebFilter) filters
				.filter((f) -> f instanceof AuthenticationWebFilter).findFirst().orElse(null);
		ReactiveAuthenticationManagerResolver<?> authenticationManagerResolver = (ReactiveAuthenticationManagerResolver<?>) ReflectionTestUtils
				.getField(webFilter, "authenticationManagerResolver");
		Object authenticationManager = authenticationManagerResolver.resolve(null).block();
		assertThat(authenticationManager).isInstanceOf(JwtReactiveAuthenticationManager.class);
	}

	private void assertFilterConfiguredWithOpaqueTokenAuthenticationManager(
			AssertableReactiveWebApplicationContext context) {
		MatcherSecurityWebFilterChain filterChain = (MatcherSecurityWebFilterChain) context
				.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		Stream<WebFilter> filters = filterChain.getWebFilters().toStream();
		AuthenticationWebFilter webFilter = (AuthenticationWebFilter) filters
				.filter((f) -> f instanceof AuthenticationWebFilter).findFirst().orElse(null);
		ReactiveAuthenticationManagerResolver<?> authenticationManagerResolver = (ReactiveAuthenticationManagerResolver<?>) ReflectionTestUtils
				.getField(webFilter, "authenticationManagerResolver");
		Object authenticationManager = authenticationManagerResolver.resolve(null).block();
		assertThat(authenticationManager).isInstanceOf(OpaqueTokenReactiveAuthenticationManager.class);
	}

	private String cleanIssuerPath(String issuer) {
		if (issuer.endsWith("/")) {
			return issuer.substring(0, issuer.length() - 1);
		}
		return issuer;
	}

	private void setupMockResponse(String issuer) throws JsonProcessingException {
		MockResponse mockResponse = new MockResponse().setResponseCode(HttpStatus.OK.value())
				.setBody(new ObjectMapper().writeValueAsString(getResponse(issuer)))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		this.server.enqueue(mockResponse);
	}

	private void setupMockResponsesWithErrors(String issuer, int errorResponseCount) throws JsonProcessingException {
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
		response.put("jwks_uri", "https://example.com/oauth2/v3/certs");
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

	@EnableWebFluxSecurity
	static class TestConfig {

		@Bean
		MapReactiveUserDetailsService userDetailsService() {
			return mock(MapReactiveUserDetailsService.class);
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
		SecurityWebFilterChain testSpringSecurityFilterChain(ServerHttpSecurity http) throws Exception {
			http.authorizeExchange((exchanges) -> {
				exchanges.pathMatchers("/message/**").hasRole("ADMIN");
				exchanges.anyExchange().authenticated();
			});
			http.httpBasic();
			return http.build();
		}

	}

}
