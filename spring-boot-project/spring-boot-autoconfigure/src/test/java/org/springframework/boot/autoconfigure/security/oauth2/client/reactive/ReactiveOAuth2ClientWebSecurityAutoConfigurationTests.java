/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client.reactive;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.OAuth2AuthorizationCodeGrantWebFilter;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.authentication.OAuth2LoginAuthenticationWebFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveOAuth2ClientWebSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class ReactiveOAuth2ClientWebSecurityAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ReactiveOAuth2ClientWebSecurityAutoConfiguration.class,
				ReactiveSecurityAutoConfiguration.class));

	@Test
	void autoConfigurationShouldBackOffForServletEnvironments() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveOAuth2ClientWebSecurityAutoConfiguration.class))
			.run((context) -> assertThat(context)
				.doesNotHaveBean(ReactiveOAuth2ClientWebSecurityAutoConfiguration.class));
	}

	@Test
	void autoConfigurationIsConditionalOnAuthorizedClientService() {
		this.contextRunner.run((context) -> assertThat(context)
			.doesNotHaveBean(ReactiveOAuth2ClientWebSecurityAutoConfiguration.class));
	}

	@Test
	void configurationRegistersAuthorizedClientRepositoryBean() {
		this.contextRunner.withUserConfiguration(ReactiveOAuth2AuthorizedClientServiceConfiguration.class)
			.run((context) -> assertThat(context)
				.hasSingleBean(AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository.class));
	}

	@Test
	void authorizedClientRepositoryBeanIsConditionalOnMissingBean() {
		this.contextRunner.withUserConfiguration(ReactiveOAuth2AuthorizedClientRepositoryConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(ServerOAuth2AuthorizedClientRepository.class);
				assertThat(context).hasBean("testAuthorizedClientRepository");
			});
	}

	@Test
	void configurationRegistersSecurityWebFilterChainBean() { // gh-17949
		this.contextRunner
			.withUserConfiguration(ReactiveOAuth2AuthorizedClientServiceConfiguration.class,
					ServerHttpSecurityConfiguration.class)
			.run((context) -> {
				assertThat(hasFilter(context, OAuth2LoginAuthenticationWebFilter.class)).isTrue();
				assertThat(hasFilter(context, OAuth2AuthorizationCodeGrantWebFilter.class)).isTrue();
			});
	}

	@Test
	void securityWebFilterChainBeanConditionalOnWebApplication() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveOAuth2ClientWebSecurityAutoConfiguration.class,
					ReactiveSecurityAutoConfiguration.class))
			.withUserConfiguration(ReactiveOAuth2AuthorizedClientRepositoryConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(SecurityWebFilterChain.class));
	}

	@SuppressWarnings("unchecked")
	private boolean hasFilter(AssertableReactiveWebApplicationContext context, Class<? extends WebFilter> filter) {
		SecurityWebFilterChain filterChain = (SecurityWebFilterChain) context
			.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<WebFilter> filters = (List<WebFilter>) ReflectionTestUtils.getField(filterChain, "filters");
		return filters.stream().anyMatch(filter::isInstance);
	}

	@Configuration(proxyBeanMethods = false)
	@Import(ReactiveClientRepositoryConfiguration.class)
	static class ReactiveOAuth2AuthorizedClientServiceConfiguration {

		@Bean
		InMemoryReactiveOAuth2AuthorizedClientService testAuthorizedClientService(
				ReactiveClientRegistrationRepository clientRegistrationRepository) {
			return new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrationRepository);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ReactiveOAuth2AuthorizedClientServiceConfiguration.class)
	static class ReactiveOAuth2AuthorizedClientRepositoryConfiguration {

		@Bean
		ServerOAuth2AuthorizedClientRepository testAuthorizedClientRepository(
				ReactiveOAuth2AuthorizedClientService authorizedClientService) {
			return new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(authorizedClientService);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveClientRepositoryConfiguration {

		@Bean
		ReactiveClientRegistrationRepository clientRegistrationRepository() {
			List<ClientRegistration> registrations = new ArrayList<>();
			registrations.add(getClientRegistration("first", "https://user-info-uri.com"));
			registrations.add(getClientRegistration("second", "https://other-user-info"));
			return new InMemoryReactiveClientRegistrationRepository(registrations);
		}

		private ClientRegistration getClientRegistration(String id, String userInfoUri) {
			ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(id);
			builder.clientName("foo")
				.clientId("foo")
				.clientAuthenticationMethod(
						org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.scope("read")
				.clientSecret("secret")
				.redirectUri("https://redirect-uri.com")
				.authorizationUri("https://authorization-uri.com")
				.tokenUri("https://token-uri.com")
				.userInfoUri(userInfoUri)
				.userNameAttributeName("login");
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServerHttpSecurityConfiguration {

		@Bean
		ServerHttpSecurity http() {
			TestServerHttpSecurity httpSecurity = new TestServerHttpSecurity();
			return httpSecurity;
		}

		static class TestServerHttpSecurity extends ServerHttpSecurity implements ApplicationContextAware {

			@Override
			public void setApplicationContext(ApplicationContext applicationContext) {
				super.setApplicationContext(applicationContext);
			}

		}

	}

}
