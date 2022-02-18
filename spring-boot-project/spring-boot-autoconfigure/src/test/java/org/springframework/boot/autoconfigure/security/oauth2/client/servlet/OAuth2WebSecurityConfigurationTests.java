/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationCodeGrantFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2WebSecurityConfiguration}.
 *
 * @author Madhura Bhave
 */
class OAuth2WebSecurityConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void securityConfigurerConfiguresOAuth2Login() {
		this.contextRunner.withUserConfiguration(ClientRegistrationRepositoryConfiguration.class,
				OAuth2WebSecurityConfiguration.class).run((context) -> {
					ClientRegistrationRepository expected = context.getBean(ClientRegistrationRepository.class);
					ClientRegistrationRepository actual = (ClientRegistrationRepository) ReflectionTestUtils.getField(
							getFilters(context, OAuth2LoginAuthenticationFilter.class).get(0),
							"clientRegistrationRepository");
					assertThat(isEqual(expected.findByRegistrationId("first"), actual.findByRegistrationId("first")))
							.isTrue();
					assertThat(isEqual(expected.findByRegistrationId("second"), actual.findByRegistrationId("second")))
							.isTrue();
				});
	}

	@Test
	void securityConfigurerConfiguresAuthorizationCode() {
		this.contextRunner.withUserConfiguration(ClientRegistrationRepositoryConfiguration.class,
				OAuth2WebSecurityConfiguration.class).run((context) -> {
					ClientRegistrationRepository expected = context.getBean(ClientRegistrationRepository.class);
					ClientRegistrationRepository actual = (ClientRegistrationRepository) ReflectionTestUtils.getField(
							getFilters(context, OAuth2AuthorizationCodeGrantFilter.class).get(0),
							"clientRegistrationRepository");
					assertThat(isEqual(expected.findByRegistrationId("first"), actual.findByRegistrationId("first")))
							.isTrue();
					assertThat(isEqual(expected.findByRegistrationId("second"), actual.findByRegistrationId("second")))
							.isTrue();
				});
	}

	@Test
	void securityConfigurerBacksOffWhenClientRegistrationBeanAbsent() {
		this.contextRunner.withUserConfiguration(TestConfig.class, OAuth2WebSecurityConfiguration.class)
				.run((context) -> {
					assertThat(getFilters(context, OAuth2LoginAuthenticationFilter.class)).isEmpty();
					assertThat(getFilters(context, OAuth2AuthorizationCodeGrantFilter.class)).isEmpty();
				});
	}

	@Test
	void configurationRegistersAuthorizedClientServiceBean() {
		this.contextRunner
				.withUserConfiguration(ClientRegistrationRepositoryConfiguration.class,
						OAuth2WebSecurityConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(OAuth2AuthorizedClientService.class));
	}

	@Test
	void configurationRegistersAuthorizedClientRepositoryBean() {
		this.contextRunner
				.withUserConfiguration(ClientRegistrationRepositoryConfiguration.class,
						OAuth2WebSecurityConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(OAuth2AuthorizedClientRepository.class));
	}

	@Test
	void securityFilterChainConfigBacksOffWhenOtherWebSecurityAdapterPresent() {
		this.contextRunner
				.withUserConfiguration(TestWebSecurityConfigurerConfig.class, OAuth2WebSecurityConfiguration.class)
				.run((context) -> {
					assertThat(getFilters(context, OAuth2LoginAuthenticationFilter.class)).isEmpty();
					assertThat(getFilters(context, OAuth2AuthorizationCodeGrantFilter.class)).isEmpty();
					assertThat(context).getBean(OAuth2AuthorizedClientService.class).isNotNull();
				});
	}

	@Test
	void securityFilterChainConfigBacksOffWhenOtherSecurityFilterChainBeanPresent() {
		this.contextRunner
				.withUserConfiguration(TestSecurityFilterChainConfig.class, OAuth2WebSecurityConfiguration.class)
				.run((context) -> {
					assertThat(getFilters(context, OAuth2LoginAuthenticationFilter.class)).isEmpty();
					assertThat(getFilters(context, OAuth2AuthorizationCodeGrantFilter.class)).isEmpty();
					assertThat(context).getBean(OAuth2AuthorizedClientService.class).isNotNull();
				});
	}

	@Test
	void securityFilterChainConfigConditionalOnSecurityFilterChainClass() {
		this.contextRunner
				.withUserConfiguration(ClientRegistrationRepositoryConfiguration.class,
						OAuth2WebSecurityConfiguration.class)
				.withClassLoader(new FilteredClassLoader(SecurityFilterChain.class)).run((context) -> {
					assertThat(getFilters(context, OAuth2LoginAuthenticationFilter.class)).isEmpty();
					assertThat(getFilters(context, OAuth2AuthorizationCodeGrantFilter.class)).isEmpty();
				});
	}

	@Test
	void authorizedClientServiceBeanIsConditionalOnMissingBean() {
		this.contextRunner.withUserConfiguration(OAuth2AuthorizedClientServiceConfiguration.class,
				OAuth2WebSecurityConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(OAuth2AuthorizedClientService.class);
					assertThat(context).hasBean("testAuthorizedClientService");
				});
	}

	@Test
	void authorizedClientRepositoryBeanIsConditionalOnMissingBean() {
		this.contextRunner.withUserConfiguration(OAuth2AuthorizedClientRepositoryConfiguration.class,
				OAuth2WebSecurityConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(OAuth2AuthorizedClientRepository.class);
					assertThat(context).hasBean("testAuthorizedClientRepository");
				});
	}

	private List<Filter> getFilters(AssertableApplicationContext context, Class<? extends Filter> filter) {
		FilterChainProxy filterChain = (FilterChainProxy) context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<SecurityFilterChain> filterChains = filterChain.getFilterChains();
		List<Filter> filters = filterChains.get(0).getFilters();
		return filters.stream().filter(filter::isInstance).collect(Collectors.toList());
	}

	private boolean isEqual(ClientRegistration reg1, ClientRegistration reg2) {
		boolean result = ObjectUtils.nullSafeEquals(reg1.getClientId(), reg2.getClientId());
		result = result && ObjectUtils.nullSafeEquals(reg1.getClientName(), reg2.getClientName());
		result = result && ObjectUtils.nullSafeEquals(reg1.getClientSecret(), reg2.getClientSecret());
		result = result && ObjectUtils.nullSafeEquals(reg1.getScopes(), reg2.getScopes());
		result = result && ObjectUtils.nullSafeEquals(reg1.getRedirectUri(), reg2.getRedirectUri());
		result = result && ObjectUtils.nullSafeEquals(reg1.getRegistrationId(), reg2.getRegistrationId());
		result = result
				&& ObjectUtils.nullSafeEquals(reg1.getAuthorizationGrantType(), reg2.getAuthorizationGrantType());
		result = result && ObjectUtils.nullSafeEquals(reg1.getProviderDetails().getAuthorizationUri(),
				reg2.getProviderDetails().getAuthorizationUri());
		result = result && ObjectUtils.nullSafeEquals(reg1.getProviderDetails().getUserInfoEndpoint(),
				reg2.getProviderDetails().getUserInfoEndpoint());
		result = result && ObjectUtils.nullSafeEquals(reg1.getProviderDetails().getTokenUri(),
				reg2.getProviderDetails().getTokenUri());
		return result;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	static class TestConfig {

		@Bean
		TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestConfig.class)
	static class ClientRegistrationRepositoryConfiguration {

		@Bean
		ClientRegistrationRepository clientRegistrationRepository() {
			List<ClientRegistration> registrations = new ArrayList<>();
			registrations.add(getClientRegistration("first", "https://user-info-uri.com"));
			registrations.add(getClientRegistration("second", "https://other-user-info"));
			return new InMemoryClientRegistrationRepository(registrations);
		}

		private ClientRegistration getClientRegistration(String id, String userInfoUri) {
			ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(id);
			builder.clientName("foo").clientId("foo")
					.clientAuthenticationMethod(
							org.springframework.security.oauth2.core.ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).scope("read")
					.clientSecret("secret").redirectUri("https://redirect-uri.com")
					.authorizationUri("https://authorization-uri.com").tokenUri("https://token-uri.com")
					.userInfoUri(userInfoUri).userNameAttributeName("login");
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ClientRegistrationRepositoryConfiguration.class)
	@SuppressWarnings("deprecation")
	static class TestWebSecurityConfigurerConfig
			extends org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ClientRegistrationRepositoryConfiguration.class)
	static class TestSecurityFilterChainConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
			return http.antMatcher("/**").authorizeRequests((authorize) -> authorize.anyRequest().authenticated())
					.build();

		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ClientRegistrationRepositoryConfiguration.class)
	static class OAuth2AuthorizedClientServiceConfiguration {

		@Bean
		OAuth2AuthorizedClientService testAuthorizedClientService(
				ClientRegistrationRepository clientRegistrationRepository) {
			return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ClientRegistrationRepositoryConfiguration.class)
	static class OAuth2AuthorizedClientRepositoryConfiguration {

		@Bean
		OAuth2AuthorizedClientRepository testAuthorizedClientRepository(
				OAuth2AuthorizedClientService authorizedClientService) {
			return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
		}

	}

}
