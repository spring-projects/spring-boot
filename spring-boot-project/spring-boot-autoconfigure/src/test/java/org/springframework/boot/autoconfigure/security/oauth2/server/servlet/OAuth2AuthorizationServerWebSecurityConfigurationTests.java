/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.server.servlet;

import java.util.List;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.oidc.web.OidcClientRegistrationEndpointFilter;
import org.springframework.security.oauth2.server.authorization.oidc.web.OidcProviderConfigurationEndpointFilter;
import org.springframework.security.oauth2.server.authorization.oidc.web.OidcUserInfoEndpointFilter;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.web.OAuth2AuthorizationEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.OAuth2AuthorizationServerMetadataEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.OAuth2TokenEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.OAuth2TokenIntrospectionEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.OAuth2TokenRevocationEndpointFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Tests for {@link OAuth2AuthorizationServerWebSecurityConfiguration}.
 *
 * @author Steve Riesenberg
 */
class OAuth2AuthorizationServerWebSecurityConfigurationTests {

	private static final String PROPERTIES_PREFIX = "spring.security.oauth2.authorizationserver";

	private static final String CLIENT_PREFIX = PROPERTIES_PREFIX + ".client";

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner();

	@Test
	void webSecurityConfigurationConfiguresAuthorizationServerWithFormLogin() {
		this.contextRunner.withUserConfiguration(TestOAuth2AuthorizationServerConfiguration.class)
			.withPropertyValues(CLIENT_PREFIX + ".foo.registration.client-id=abcd",
					CLIENT_PREFIX + ".foo.registration.client-secret=secret",
					CLIENT_PREFIX + ".foo.registration.client-authentication-methods=client_secret_basic",
					CLIENT_PREFIX + ".foo.registration.authorization-grant-types=client_credentials",
					CLIENT_PREFIX + ".foo.registration.scopes=test")
			.run((context) -> {
				assertThat(context).hasBean("authorizationServerSecurityFilterChain");
				assertThat(context).hasBean("defaultSecurityFilterChain");
				assertThat(context).hasBean("registeredClientRepository");
				assertThat(context).hasBean("authorizationServerSettings");
				assertThat(findFilter(context, OAuth2AuthorizationEndpointFilter.class, 0)).isNotNull();
				assertThat(findFilter(context, OAuth2TokenEndpointFilter.class, 0)).isNotNull();
				assertThat(findFilter(context, OAuth2TokenIntrospectionEndpointFilter.class, 0)).isNotNull();
				assertThat(findFilter(context, OAuth2TokenRevocationEndpointFilter.class, 0)).isNotNull();
				assertThat(findFilter(context, OAuth2AuthorizationServerMetadataEndpointFilter.class, 0)).isNotNull();
				assertThat(findFilter(context, OidcProviderConfigurationEndpointFilter.class, 0)).isNotNull();
				assertThat(findFilter(context, OidcUserInfoEndpointFilter.class, 0)).isNotNull();
				assertThat(findFilter(context, BearerTokenAuthenticationFilter.class, 0)).isNotNull();
				assertThat(findFilter(context, OidcClientRegistrationEndpointFilter.class, 0)).isNull();
				assertThat(findFilter(context, UsernamePasswordAuthenticationFilter.class, 0)).isNull();
				assertThat(findFilter(context, DefaultLoginPageGeneratingFilter.class, 1)).isNotNull();
				assertThat(findFilter(context, UsernamePasswordAuthenticationFilter.class, 1)).isNotNull();
			});
	}

	@Test
	void securityFilterChainsBackOffWhenSecurityFilterChainBeanPresent() {
		this.contextRunner
			.withUserConfiguration(TestSecurityFilterChainConfiguration.class,
					TestOAuth2AuthorizationServerConfiguration.class)
			.withPropertyValues(CLIENT_PREFIX + ".foo.registration.client-id=abcd",
					CLIENT_PREFIX + ".foo.registration.client-secret=secret",
					CLIENT_PREFIX + ".foo.registration.client-authentication-methods=client_secret_basic",
					CLIENT_PREFIX + ".foo.registration.authorization-grant-types=client_credentials",
					CLIENT_PREFIX + ".foo.registration.scopes=test")
			.run((context) -> {
				assertThat(context).hasBean("authServerSecurityFilterChain");
				assertThat(context).doesNotHaveBean("authorizationServerSecurityFilterChain");
				assertThat(context).hasBean("securityFilterChain");
				assertThat(context).doesNotHaveBean("defaultSecurityFilterChain");
				assertThat(context).hasBean("registeredClientRepository");
				assertThat(context).hasBean("authorizationServerSettings");
				assertThat(findFilter(context, BearerTokenAuthenticationFilter.class, 0)).isNull();
				assertThat(findFilter(context, UsernamePasswordAuthenticationFilter.class, 1)).isNull();
			});
	}

	private Filter findFilter(AssertableWebApplicationContext context, Class<? extends Filter> filter,
			int filterChainIndex) {
		FilterChainProxy filterChain = (FilterChainProxy) context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<SecurityFilterChain> filterChains = filterChain.getFilterChains();
		List<Filter> filters = filterChains.get(filterChainIndex).getFilters();
		return filters.stream().filter(filter::isInstance).findFirst().orElse(null);
	}

	@Configuration
	@EnableWebSecurity
	@Import({ TestRegisteredClientRepositoryConfiguration.class,
			OAuth2AuthorizationServerWebSecurityConfiguration.class,
			OAuth2AuthorizationServerJwtAutoConfiguration.class })
	static class TestOAuth2AuthorizationServerConfiguration {

	}

	@Configuration
	static class TestRegisteredClientRepositoryConfiguration {

		@Bean
		RegisteredClientRepository registeredClientRepository() {
			RegisteredClient registeredClient = RegisteredClient.withId("test")
				.clientId("abcd")
				.clientSecret("secret")
				.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
				.scope("test")
				.build();
			return new InMemoryRegisteredClientRepository(registeredClient);
		}

		@Bean
		AuthorizationServerSettings authorizationServerSettings() {
			return AuthorizationServerSettings.builder().issuer("https://example.com").build();
		}

	}

	@Configuration
	@EnableWebSecurity
	static class TestSecurityFilterChainConfiguration {

		@Bean
		@Order(1)
		SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {
			OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
			return http.build();
		}

		@Bean
		@Order(2)
		SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
			return http.httpBasic(withDefaults()).build();
		}

	}

}
