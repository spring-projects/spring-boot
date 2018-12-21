/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.security.oauth2.resource.servlet;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;

import com.nimbusds.jose.JWSAlgorithm;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OAuth2ResourceServerAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Artsiom Yudovin
 */
public class OAuth2ResourceServerAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class))
			.withUserConfiguration(TestConfig.class);

	private MockWebServer server;

	@After
	public void cleanup() throws Exception {
		if (this.server != null) {
			this.server.shutdown();
		}
	}

	@Test
	public void autoConfigurationShouldConfigureResourceServer() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com")
				.run((context) -> {
					assertThat(context.getBean(JwtDecoder.class))
							.isInstanceOf(NimbusJwtDecoderJwkSupport.class);
					assertThat(getBearerTokenFilter(context)).isNotNull();
				});
	}

	@Test
	public void autoConfigurationShouldMatchDefaultJwsAlgorithm() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com")
				.run((context) -> {
					JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
					assertThat(jwtDecoder).hasFieldOrPropertyWithValue("jwsAlgorithm",
							JWSAlgorithm.RS256);
				});
	}

	@Test
	public void autoConfigurationShouldConfigureResourceServerWithJwsAlgorithm() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com",
				"spring.security.oauth2.resourceserver.jwt.jws-algorithm=HS512")
				.run((context) -> {
					JwtDecoder jwtDecoder = context.getBean(JwtDecoder.class);
					assertThat(jwtDecoder).hasFieldOrPropertyWithValue("jwsAlgorithm",
							JWSAlgorithm.HS512);
					assertThat(getBearerTokenFilter(context)).isNotNull();
				});
	}

	@Test
	public void autoConfigurationShouldConfigureResourceServerUsingOidcIssuerUri()
			throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		String issuer = this.server.url("").toString();
		String cleanIssuerPath = cleanIssuerPath(issuer);
		setupMockResponse(cleanIssuerPath);
		this.contextRunner
				.withPropertyValues(
						"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://"
								+ this.server.getHostName() + ":" + this.server.getPort())
				.run((context) -> {
					assertThat(context.getBean(JwtDecoder.class))
							.isInstanceOf(NimbusJwtDecoderJwkSupport.class);
					assertThat(getBearerTokenFilter(context)).isNotNull();
				});
	}

	@Test
	public void autoConfigurationWhenBothSetUriAndIssuerUriPresentShouldUseSetUri() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://issuer-uri.com",
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com")
				.run((context) -> {
					assertThat(context.getBean(JwtDecoder.class))
							.isInstanceOf(NimbusJwtDecoderJwkSupport.class);
					assertThat(getBearerTokenFilter(context)).isNotNull();
					assertThat(context.containsBean("jwtDecoderByJwkKeySetUri")).isTrue();
					assertThat(context.containsBean("jwtDecoderByOidcIssuerUri"))
							.isFalse();
				});
	}

	@Test
	public void autoConfigurationWhenJwkSetUriNullShouldNotFail() {
		this.contextRunner
				.run((context) -> assertThat(getBearerTokenFilter(context)).isNull());
	}

	@Test
	public void jwtDecoderByJwkSetUriIsConditionalOnMissingBean() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com")
				.withUserConfiguration(JwtDecoderConfig.class)
				.run((context) -> assertThat(getBearerTokenFilter(context)).isNotNull());
	}

	@Test
	public void jwtDecoderByOidcIssuerUriIsConditionalOnMissingBean() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.issuer-uri=http://jwk-oidc-issuer-location.com")
				.withUserConfiguration(JwtDecoderConfig.class)
				.run((context) -> assertThat(getBearerTokenFilter(context)).isNotNull());
	}

	@Test
	public void autoConfigurationShouldBeConditionalOnJwtAuthenticationTokenClass() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com")
				.withUserConfiguration(JwtDecoderConfig.class)
				.withClassLoader(new FilteredClassLoader(JwtAuthenticationToken.class))
				.run((context) -> assertThat(getBearerTokenFilter(context)).isNull());
	}

	@Test
	public void autoConfigurationShouldBeConditionalOnJwtDecoderClass() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com")
				.withUserConfiguration(JwtDecoderConfig.class)
				.withClassLoader(new FilteredClassLoader(JwtDecoder.class))
				.run((context) -> assertThat(getBearerTokenFilter(context)).isNull());
	}

	private Filter getBearerTokenFilter(AssertableWebApplicationContext context) {
		FilterChainProxy filterChain = (FilterChainProxy) context
				.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<SecurityFilterChain> filterChains = filterChain.getFilterChains();
		List<Filter> filters = filterChains.get(0).getFilters();
		return filters.stream()
				.filter((f) -> f instanceof BearerTokenAuthenticationFilter).findFirst()
				.orElse(null);
	}

	private String cleanIssuerPath(String issuer) {
		if (issuer.endsWith("/")) {
			return issuer.substring(0, issuer.length() - 1);
		}
		return issuer;
	}

	private void setupMockResponse(String issuer) throws JsonProcessingException {
		MockResponse mockResponse = new MockResponse()
				.setResponseCode(HttpStatus.OK.value())
				.setBody(new ObjectMapper().writeValueAsString(getResponse(issuer)))
				.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		this.server.enqueue(mockResponse);
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
		response.put("grant_types_supported",
				Collections.singletonList("authorization_code"));
		response.put("token_endpoint", "https://example.com/oauth2/v4/token");
		response.put("token_endpoint_auth_methods_supported",
				Collections.singletonList("client_secret_basic"));
		response.put("userinfo_endpoint", "https://example.com/oauth2/v3/userinfo");
		return response;
	}

	@Configuration
	@EnableWebSecurity
	static class TestConfig {

	}

	@Configuration
	@EnableWebSecurity
	static class JwtDecoderConfig {

		@Bean
		public JwtDecoder decoder() {
			return mock(JwtDecoder.class);
		}

	}

}
