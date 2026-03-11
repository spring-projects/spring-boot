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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.web;

import java.util.List;

import jakarta.servlet.Filter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link OAuth2ResourceServerWebSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
class OAuth2ResourceServerWebSecurityAutoConfigurationTests {

	private static final String JWK_SET_URI_PROPERTY = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://authserver";

	private static final String MANAGEMENT_SECURITY_FILTER_CHAIN_BEAN = "managementSecurityFilterChain";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ManagementWebSecurityAutoConfiguration.class,
				OAuth2ResourceServerAutoConfiguration.class, OAuth2ResourceServerWebSecurityAutoConfiguration.class,
				SecurityAutoConfiguration.class, ServletWebSecurityAutoConfiguration.class,
				WebMvcAutoConfiguration.class));

	@Test
	void causesManagementWebSecurityAutoConfigurationToBackOff() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(ManagementWebSecurityAutoConfiguration.class));
		this.contextRunner.withPropertyValues(JWK_SET_URI_PROPERTY)
			.run((context) -> assertThat(context).doesNotHaveBean(ManagementWebSecurityAutoConfiguration.class)
				.doesNotHaveBean(MANAGEMENT_SECURITY_FILTER_CHAIN_BEAN));
	}

	@Test
	void whenNoJwtDecoderDoesNotAddFilterChain() {
		this.contextRunner.run((context) -> assertThat(getBearerTokenFilter(context)).isNull());
	}

	@Test
	void whenHasJwtDecoderAddsFilterChain() {
		this.contextRunner.withPropertyValues(JWK_SET_URI_PROPERTY).run((context) -> {
			Filter bearerTokenFilter = getBearerTokenFilter(context);
			assertThat(bearerTokenFilter).isNotNull();
			assertThat(getAuthenticationProviders(bearerTokenFilter))
				.hasAtLeastOneElementOfType(JwtAuthenticationProvider.class);
		});
	}

	@Test
	void whenNoOpaqueTokenIntrospectorDoesNotAddFilterChain() {
		this.contextRunner.run((context) -> assertThat(getBearerTokenFilter(context)).isNull());
	}

	@Test
	void whenHasOpaqueTokenIntrospectorAddsFilterChain() {
		this.contextRunner
			.withPropertyValues(
					"spring.security.oauth2.resourceserver.opaquetoken.introspection-uri=https://authserver",
					"spring.security.oauth2.resourceserver.opaquetoken.client-id=test",
					"spring.security.oauth2.resourceserver.opaquetoken.client-secret=shh")
			.run((context) -> {
				Filter bearerTokenFilter = getBearerTokenFilter(context);
				assertThat(bearerTokenFilter).isNotNull();
				assertThat(getAuthenticationProviders(bearerTokenFilter))
					.hasAtLeastOneElementOfType(OpaqueTokenAuthenticationProvider.class);
			});
	}

	private @Nullable Filter getBearerTokenFilter(ApplicationContext context) {
		return getFilters(context).stream()
			.filter(BearerTokenAuthenticationFilter.class::isInstance)
			.findFirst()
			.orElse(null);
	}

	private List<Filter> getFilters(ApplicationContext context) {
		FilterChainProxy filterChain = (FilterChainProxy) context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<SecurityFilterChain> filterChains = filterChain.getFilterChains();
		return filterChains.get(0).getFilters();
	}

	private List<AuthenticationProvider> getAuthenticationProviders(Filter bearerTokenFilter) {
		AuthenticationManagerResolver<?> resolver = (AuthenticationManagerResolver<?>) ReflectionTestUtils
			.getField(bearerTokenFilter, "authenticationManagerResolver");
		assertThat(resolver).isNotNull();
		ProviderManager providerManager = (ProviderManager) resolver.resolve(mock());
		return providerManager.getProviders();
	}

}
