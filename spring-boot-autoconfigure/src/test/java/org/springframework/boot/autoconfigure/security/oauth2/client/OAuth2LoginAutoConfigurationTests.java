/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.Filter;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.client.authentication.AuthorizationCodeAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.authentication.AuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.authentication.AuthorizationCodeRequestRedirectFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.user.nimbus.NimbusOAuth2UserService;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2LoginAutoConfiguration}.
 *
 * @author Joe Grandja
 */
public class OAuth2LoginAutoConfigurationTests {
	private static final String CLIENT_PROPERTY_PREFIX = "security.oauth2.client";

	private static final String CLIENT_ID_PROPERTY = "client-id";

	private static final String CLIENT_SECRET_PROPERTY = "client-secret";

	private static final String GOOGLE_CLIENT_KEY = "google";

	private static final String GOOGLE_CLIENT_PROPERTY_BASE = CLIENT_PROPERTY_PREFIX + "."
			+ GOOGLE_CLIENT_KEY;

	private static final String GITHUB_CLIENT_KEY = "github";

	private static final String GITHUB_CLIENT_PROPERTY_BASE = CLIENT_PROPERTY_PREFIX + "."
			+ GITHUB_CLIENT_KEY;

	private static final String FACEBOOK_CLIENT_KEY = "facebook";

	private static final String FACEBOOK_CLIENT_PROPERTY_BASE = CLIENT_PROPERTY_PREFIX
			+ "." + FACEBOOK_CLIENT_KEY;

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void refreshContextWhenNoOverridesThenLoadDefaultConfiguration()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		// Prepare environment
		TestPropertyValues
				.of(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=google-client-id")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=google-client-secret")
				.and(GITHUB_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=github-client-id")
				.and(GITHUB_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=github-client-secret")
				.and(FACEBOOK_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=facebook-client-id")
				.and(FACEBOOK_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=facebook-client-secret")
				.applyTo(this.context.getEnvironment());

		this.context.refresh();

		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNotNull();
		assertThat(clientRegistrationRepository.getRegistrations().size()).isEqualTo(3);

		List<Filter> oauth2SecurityFilters = this.assertOAuth2SecurityFiltersConfigured();

		AuthorizationCodeRequestRedirectFilter authorizationCodeRequestRedirectFilter = (AuthorizationCodeRequestRedirectFilter) oauth2SecurityFilters
				.get(0);
		AuthorizationCodeAuthenticationProcessingFilter authorizationCodeAuthenticationProcessingFilter = (AuthorizationCodeAuthenticationProcessingFilter) oauth2SecurityFilters
				.get(1);

		assertThat(this.reflectClientRegistrationRepository(
				authorizationCodeRequestRedirectFilter))
						.isSameAs(clientRegistrationRepository);
		assertThat(this.reflectClientRegistrationRepository(
				authorizationCodeAuthenticationProcessingFilter))
						.isSameAs(clientRegistrationRepository);

		AuthorizationCodeAuthenticationProvider authorizationCodeAuthenticationProvider = this
				.reflectAuthenticationProvider(
						authorizationCodeAuthenticationProcessingFilter);
		assertThat(authorizationCodeAuthenticationProvider).isNotNull();

		NimbusOAuth2UserService nimbusOAuth2UserService = this
				.reflectOAuth2UserService(authorizationCodeAuthenticationProvider);
		assertThat(nimbusOAuth2UserService).isNotNull();

		Field userNameAttributeNamesField = ReflectionUtils
				.findField(NimbusOAuth2UserService.class, "userNameAttributeNames");
		ReflectionUtils.makeAccessible(userNameAttributeNamesField);
		Map<URI, String> userNameAttributeNames = (Map<URI, String>) ReflectionUtils
				.getField(userNameAttributeNamesField, nimbusOAuth2UserService);
		assertThat(userNameAttributeNames).isNotEmpty();

		URI gitHubUserInfoUri = URI.create("https://api.github.com/user");
		assertThat(userNameAttributeNames.containsKey(gitHubUserInfoUri)).isTrue();
		assertThat(userNameAttributeNames.get(gitHubUserInfoUri)).isEqualTo("name");

		URI facebookUserInfoUri = URI.create("https://graph.facebook.com/me");
		assertThat(userNameAttributeNames.containsKey(facebookUserInfoUri)).isTrue();
		assertThat(userNameAttributeNames.get(facebookUserInfoUri)).isEqualTo("name");
	}

	@Test
	public void refreshContextWhenGoogleClientConfiguredNoOverridesThenLoadDefaultConfiguration()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		// Prepare environment
		TestPropertyValues
				.of(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=google-client-id")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=google-client-secret")
				.applyTo(this.context.getEnvironment());

		this.context.refresh();

		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNotNull();
		assertThat(clientRegistrationRepository.getRegistrations().size()).isEqualTo(1);

		List<Filter> oauth2SecurityFilters = this.assertOAuth2SecurityFiltersConfigured();

		AuthorizationCodeRequestRedirectFilter authorizationCodeRequestRedirectFilter = (AuthorizationCodeRequestRedirectFilter) oauth2SecurityFilters
				.get(0);
		AuthorizationCodeAuthenticationProcessingFilter authorizationCodeAuthenticationProcessingFilter = (AuthorizationCodeAuthenticationProcessingFilter) oauth2SecurityFilters
				.get(1);

		assertThat(this.reflectClientRegistrationRepository(
				authorizationCodeRequestRedirectFilter))
						.isSameAs(clientRegistrationRepository);
		assertThat(this.reflectClientRegistrationRepository(
				authorizationCodeAuthenticationProcessingFilter))
						.isSameAs(clientRegistrationRepository);
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void refreshContextWhenNotWebContextThenConfigurationBacksOff()
			throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ClientRegistrationRepositoryAutoConfiguration.class,
				OAuth2LoginAutoConfiguration.class, SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		context.refresh();
		context.getBean(FilterChainProxy.class);
	}

	@Test
	public void refreshContextWhenNoClientsConfiguredThenConfigurationBacksOff()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);
		this.context.refresh();
		this.assertDefaultConfigurationBacksOff();
	}

	@Test
	public void refreshContextWhenOneInvalidClientConfiguredThenConfigurationBacksOff()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		// Prepare environment
		// NOTE: client-id is required, if not configured, the client will get filtered
		// out
		TestPropertyValues.of(GITHUB_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
				+ "=github-client-secret").applyTo(this.context.getEnvironment());

		this.context.refresh();
		this.assertDefaultConfigurationBacksOff();
	}

	@Test
	public void refreshContextWhenTwoInvalidClientsConfiguredThenConfigurationBacksOff()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		// Prepare environment
		// NOTE: client-id is required, if not configured, the client will get filtered
		// out
		TestPropertyValues
				.of(GITHUB_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=github-client-secret")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=google-client-secret")
				.applyTo(this.context.getEnvironment());

		this.context.refresh();
		this.assertDefaultConfigurationBacksOff();
	}

	@Test
	public void refreshContextWhenOAuth2LoginOverrideThenConfigurationBacksOff()
			throws Exception {
		this.prepareContext(CustomConfiguration.class);

		// Prepare environment
		TestPropertyValues
				.of(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=google-client-id")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=google-client-secret")
				.applyTo(this.context.getEnvironment());

		this.context.refresh();

		this.assertOAuth2SecurityFiltersConfigured();
		CustomConfiguration customConfig = this.getBean(CustomConfiguration.class);
		assertThat(customConfig).isNotNull();
		assertThat(customConfig.configured).isTrue();
	}

	private List<Filter> assertOAuth2SecurityFiltersConfigured() {
		FilterChainProxy springSecurityFilterChain = this.getBean(FilterChainProxy.class);
		assertThat(springSecurityFilterChain).isNotNull();
		assertThat(springSecurityFilterChain.getFilterChains().size()).isEqualTo(1);

		DefaultSecurityFilterChain securityFilterChain = (DefaultSecurityFilterChain) springSecurityFilterChain
				.getFilterChains().get(0);
		assertThat(securityFilterChain.getRequestMatcher())
				.isSameAs(AnyRequestMatcher.INSTANCE);
		List<Filter> oauth2SecurityFilters = securityFilterChain.getFilters().stream()
				.filter(e -> e.getClass()
						.equals(AuthorizationCodeRequestRedirectFilter.class)
						|| e.getClass().equals(
								AuthorizationCodeAuthenticationProcessingFilter.class))
				.collect(Collectors.toList());
		assertThat(oauth2SecurityFilters).isNotEmpty();
		assertThat(oauth2SecurityFilters.size()).isEqualTo(2);
		assertThat(oauth2SecurityFilters.get(0))
				.isInstanceOf(AuthorizationCodeRequestRedirectFilter.class);
		assertThat(oauth2SecurityFilters.get(1))
				.isInstanceOf(AuthorizationCodeAuthenticationProcessingFilter.class);

		return oauth2SecurityFilters;
	}

	private void assertDefaultConfigurationBacksOff() {
		FilterChainProxy springSecurityFilterChain = this.getBean(FilterChainProxy.class);
		assertThat(springSecurityFilterChain).isNotNull(); // Boot security configuration
		// in effect

		springSecurityFilterChain.getFilterChains().forEach(chain -> {
			List<Filter> oauth2SecurityFilters = chain.getFilters().stream().filter(e -> e
					.getClass().equals(AuthorizationCodeRequestRedirectFilter.class)
					|| e.getClass().equals(
							AuthorizationCodeAuthenticationProcessingFilter.class))
					.collect(Collectors.toList());
			assertThat(oauth2SecurityFilters).isEmpty();
		});
	}

	private ClientRegistrationRepository reflectClientRegistrationRepository(
			AuthorizationCodeRequestRedirectFilter authorizationRedirectFilter) {

		Field clientRegistrationRepositoryField = ReflectionUtils.findField(
				AuthorizationCodeRequestRedirectFilter.class,
				"clientRegistrationRepository");
		ReflectionUtils.makeAccessible(clientRegistrationRepositoryField);
		return (ClientRegistrationRepository) ReflectionUtils
				.getField(clientRegistrationRepositoryField, authorizationRedirectFilter);
	}

	private ClientRegistrationRepository reflectClientRegistrationRepository(
			AuthorizationCodeAuthenticationProcessingFilter authenticationProcessingFilter) {

		Field clientRegistrationRepositoryField = ReflectionUtils.findField(
				AuthorizationCodeAuthenticationProcessingFilter.class,
				"clientRegistrationRepository");
		ReflectionUtils.makeAccessible(clientRegistrationRepositoryField);
		return (ClientRegistrationRepository) ReflectionUtils.getField(
				clientRegistrationRepositoryField, authenticationProcessingFilter);
	}

	private AuthorizationCodeAuthenticationProvider reflectAuthenticationProvider(
			AuthorizationCodeAuthenticationProcessingFilter authenticationProcessingFilter) {

		Field authenticationMangerField = ReflectionUtils.findField(
				AuthorizationCodeAuthenticationProcessingFilter.class.getSuperclass(),
				"authenticationManager");
		ReflectionUtils.makeAccessible(authenticationMangerField);
		ProviderManager authenticationManager = (ProviderManager) ReflectionUtils
				.getField(authenticationMangerField, authenticationProcessingFilter);

		return (AuthorizationCodeAuthenticationProvider) authenticationManager
				.getProviders().stream()
				.filter(e -> e.getClass()
						.equals(AuthorizationCodeAuthenticationProvider.class))
				.findFirst().orElseGet(null);
	}

	private NimbusOAuth2UserService reflectOAuth2UserService(
			AuthorizationCodeAuthenticationProvider authenticationProvider) {
		Field userInfoServiceField = ReflectionUtils.findField(
				AuthorizationCodeAuthenticationProvider.class, "userInfoService");
		ReflectionUtils.makeAccessible(userInfoServiceField);
		return (NimbusOAuth2UserService) ReflectionUtils.getField(userInfoServiceField,
				authenticationProvider);
	}

	private void prepareContext(Class<?>... configurationClasses) {
		this.context = new AnnotationConfigWebApplicationContext();
		if (configurationClasses != null) {
			this.context.register(configurationClasses);
		}
		this.context.register(ClientRegistrationRepositoryAutoConfiguration.class,
				OAuth2LoginAutoConfiguration.class, SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
	}

	private <T> T getBean(Class<T> requiredType) {
		try {
			return this.context.getBean(requiredType);
		}
		catch (BeansException ex) {
			return null;
		}
	}

	@Configuration
	protected static class DefaultConfiguration {
	}

	@EnableWebSecurity
	protected static class CustomConfiguration extends WebSecurityConfigurerAdapter {
		private boolean configured;

		// @formatter:off
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
				.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.oauth2Login();
			this.configured = true;
		}
		// @formatter:on
	}
}
