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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.reactive;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerResolver;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtReactiveAuthenticationManager;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenReactiveAuthenticationManager;
import org.springframework.security.web.server.MatcherSecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveOAuth2ResourceServerWebSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class ReactiveOAuth2ResourceServerWebSecurityAutoConfigurationTests {

	private static final Duration TIMEOUT = Duration.ofSeconds(5000000);

	private static final String JWK_SET_URI_PROPERTY = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://authserver";

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ReactiveManagementWebSecurityAutoConfiguration.class,
				ReactiveOAuth2ResourceServerAutoConfiguration.class,
				ReactiveOAuth2ResourceServerWebSecurityAutoConfiguration.class,
				ReactiveWebSecurityAutoConfiguration.class, WebFluxAutoConfiguration.class));

	@Test
	void causesReactiveManagementWebSecurityAutoConfigurationToBackOff() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(ReactiveManagementWebSecurityAutoConfiguration.class));
		this.contextRunner.withPropertyValues(JWK_SET_URI_PROPERTY)
			.run((context) -> assertThat(context)
				.doesNotHaveBean(ReactiveManagementWebSecurityAutoConfiguration.class));
	}

	@Test
	void whenNoReactiveJwtDecoderDoesNotAddFilterChain() {
		this.contextRunner.run((context) -> {
			ReactiveAuthenticationManager authenticationManager = getAuthenticationManager(context);
			assertThatExceptionOfType(UsernameNotFoundException.class)
				.isThrownBy(authenticationManager.authenticate(mock())::block);
		});
	}

	@Test
	void whenHasReactiveJwtDecoderAddsFilterChain() {
		this.contextRunner.withPropertyValues(JWK_SET_URI_PROPERTY).run((context) -> {
			ReactiveAuthenticationManager authenticationManager = getAuthenticationManager(context);
			assertThat(authenticationManager).isInstanceOf(JwtReactiveAuthenticationManager.class);
		});
	}

	@Test
	void whenNoReactiveOpaqueTokenIntrospectorDoesNotAddFilterChain() {
		this.contextRunner.run((context) -> {
			ReactiveAuthenticationManager authenticationManager = getAuthenticationManager(context);
			assertThatExceptionOfType(UsernameNotFoundException.class)
				.isThrownBy(authenticationManager.authenticate(mock())::block);
		});
	}

	@Test
	void whenHasReactiveOpaqueTokenIntrospectorAddsFilterChain() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://authserver",
					"spring.security.oauth2.resourceserver.opaquetoken.client-id=test",
					"spring.security.oauth2.resourceserver.opaquetoken.client-secret=shh")
			.run((context) -> {
				ReactiveAuthenticationManager authenticationManager = getAuthenticationManager(context);
				assertThat(authenticationManager).isInstanceOf(OpaqueTokenReactiveAuthenticationManager.class);
			});
	}

	private ReactiveAuthenticationManager getAuthenticationManager(AssertableReactiveWebApplicationContext context) {
		AuthenticationWebFilter authenticationWebFilter = getAuthenticationWebFilter(context);
		assertThat(authenticationWebFilter).isNotNull();
		return getAuthenticationManager(authenticationWebFilter);
	}

	private @Nullable AuthenticationWebFilter getAuthenticationWebFilter(ApplicationContext context) {
		MatcherSecurityWebFilterChain filterChain = context.getBean(MatcherSecurityWebFilterChain.class);
		return (AuthenticationWebFilter) filterChain.getWebFilters()
			.toStream()
			.filter(AuthenticationWebFilter.class::isInstance)
			.findFirst()
			.orElse(null);
	}

	@SuppressWarnings("unchecked")
	private ReactiveAuthenticationManager getAuthenticationManager(AuthenticationWebFilter webFilter) {
		ReactiveAuthenticationManagerResolver<ServerWebExchange> authenticationManagerResolver = (ReactiveAuthenticationManagerResolver<ServerWebExchange>) ReflectionTestUtils
			.getField(webFilter, "authenticationManagerResolver");
		assertThat(authenticationManagerResolver).isNotNull();
		ReactiveAuthenticationManager authenticationManager = authenticationManagerResolver
			.resolve(mock(ServerWebExchange.class))
			.block(TIMEOUT);
		assertThat(authenticationManager).isNotNull();
		return authenticationManager;
	}

}
