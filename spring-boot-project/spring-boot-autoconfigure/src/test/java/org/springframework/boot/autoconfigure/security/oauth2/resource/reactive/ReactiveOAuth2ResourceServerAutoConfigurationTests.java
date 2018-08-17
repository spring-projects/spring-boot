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
package org.springframework.boot.autoconfigure.security.oauth2.resource.reactive;

import java.util.List;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
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
 */
public class ReactiveOAuth2ResourceServerAutoConfigurationTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations
					.of(ReactiveOAuth2ResourceServerAutoConfiguration.class))
			.withUserConfiguration(TestConfig.class);

	@Test
	public void autoConfigurationShouldConfigureResourceServer() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resource.jwt.jwk.set-uri=http://jwk-set-uri.com")
				.run((context) -> {
					assertThat(context.getBean(ReactiveJwtDecoder.class))
							.isInstanceOf(NimbusReactiveJwtDecoder.class);
					assertFilterConfiguredWithJwtAuthenticationManager(context);
				});
	}

	@Test
	public void autoConfigurationWhenJwkSetUriNullShouldNotFail() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN));
	}

	@Test
	public void jwtDecoderBeanIsConditionalOnMissingBean() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resource.jwt.jwk.set-uri=http://jwk-set-uri.com")
				.withUserConfiguration(JwtDecoderConfig.class)
				.run((this::assertFilterConfiguredWithJwtAuthenticationManager));
	}

	@Test
	public void autoConfigurationShouldBeConditionalOnBearerTokenAuthenticationTokenClass() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resource.jwt.jwk.set-uri=http://jwk-set-uri.com")
				.withUserConfiguration(JwtDecoderConfig.class)
				.withClassLoader(
						new FilteredClassLoader(BearerTokenAuthenticationToken.class))
				.run((context) -> assertThat(context)
						.doesNotHaveBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN));
	}

	@Test
	public void autoConfigurationWhenSecurityWebFilterChainConfigPresentShouldNotAddOne() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resource.jwt.jwk.set-uri=http://jwk-set-uri.com")
				.withUserConfiguration(SecurityWebFilterChainConfig.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(SecurityWebFilterChain.class);
					assertThat(context).hasBean("testSpringSecurityFilterChain");
				});
	}

	@SuppressWarnings("unchecked")
	private void assertFilterConfiguredWithJwtAuthenticationManager(
			AssertableReactiveWebApplicationContext context) {
		MatcherSecurityWebFilterChain filterChain = (MatcherSecurityWebFilterChain) context
				.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<WebFilter> filters = (List<WebFilter>) ReflectionTestUtils
				.getField(filterChain, "filters");
		AuthenticationWebFilter webFilter = (AuthenticationWebFilter) filters.stream()
				.filter((f) -> f instanceof AuthenticationWebFilter).findFirst()
				.orElse(null);
		ReactiveAuthenticationManager authenticationManager = (ReactiveAuthenticationManager) ReflectionTestUtils
				.getField(webFilter, "authenticationManager");
		assertThat(authenticationManager)
				.isInstanceOf(JwtReactiveAuthenticationManager.class);

	}

	@EnableWebFluxSecurity
	static class TestConfig {

		@Bean
		public MapReactiveUserDetailsService userDetailsService() {
			return mock(MapReactiveUserDetailsService.class);
		}

	}

	@Configuration
	static class JwtDecoderConfig {

		@Bean
		public ReactiveJwtDecoder decoder() {
			return mock(ReactiveJwtDecoder.class);
		}

	}

	@Configuration
	static class SecurityWebFilterChainConfig {

		@Bean
		SecurityWebFilterChain testSpringSecurityFilterChain(ServerHttpSecurity http,
				ReactiveJwtDecoder decoder) {
			http.authorizeExchange().pathMatchers("/message/**").hasRole("ADMIN")
					.anyExchange().authenticated().and().oauth2().resourceServer().jwt()
					.jwtDecoder(decoder);
			return http.build();
		}

	}

}
