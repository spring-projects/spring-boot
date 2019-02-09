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
package org.springframework.boot.autoconfigure.security.oauth2.client.reactive;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.server.AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveOAuth2ClientAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class ReactiveOAuth2ClientAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(ReactiveOAuth2ClientAutoConfiguration.class));

	private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration";

	@Test
	public void autoConfigurationShouldBackOffForServletEnvironments() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations
						.of(ReactiveOAuth2ClientAutoConfiguration.class))
				.run((context) -> assertThat(context)
						.doesNotHaveBean(ReactiveOAuth2ClientAutoConfiguration.class));
	}

	@Test
	public void clientRegistrationRepositoryBeanShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(ClientRegistrationRepository.class));
	}

	@Test
	public void clientRegistrationRepositoryBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
				REGISTRATION_PREFIX + ".foo.client-secret=secret",
				REGISTRATION_PREFIX + ".foo.provider=github").run((context) -> {
					ReactiveClientRegistrationRepository repository = context
							.getBean(ReactiveClientRegistrationRepository.class);
					ClientRegistration registration = repository
							.findByRegistrationId("foo").block(Duration.ofSeconds(30));
					assertThat(registration).isNotNull();
					assertThat(registration.getClientSecret()).isEqualTo("secret");
				});
	}

	@Test
	public void authorizedClientServiceBeanIsConditionalOnClientRegistrationRepository() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(ReactiveOAuth2AuthorizedClientService.class));
	}

	@Test
	public void configurationRegistersAuthorizedClientServiceBean() {
		this.contextRunner
				.withUserConfiguration(ReactiveClientRepositoryConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(
						InMemoryReactiveClientRegistrationRepository.class));
	}

	@Test
	public void authorizedClientServiceBeanIsConditionalOnMissingBean() {
		this.contextRunner
				.withUserConfiguration(
						ReactiveOAuth2AuthorizedClientRepositoryConfiguration.class)
				.run((context) -> {
					assertThat(context)
							.hasSingleBean(ReactiveOAuth2AuthorizedClientService.class);
					assertThat(context).hasBean("testAuthorizedClientService");
				});
	}

	@Test
	public void authorizedClientRepositoryBeanIsConditionalOnAuthorizedClientService() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(ServerOAuth2AuthorizedClientRepository.class));
	}

	@Test
	public void configurationRegistersAuthorizedClientRepositoryBean() {
		this.contextRunner
				.withUserConfiguration(
						ReactiveOAuth2AuthorizedClientServiceConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(
						AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository.class));
	}

	@Test
	public void authorizedClientRepositoryBeanIsConditionalOnMissingBean() {
		this.contextRunner
				.withUserConfiguration(
						ReactiveOAuth2AuthorizedClientRepositoryConfiguration.class)
				.run((context) -> {
					assertThat(context)
							.hasSingleBean(ServerOAuth2AuthorizedClientRepository.class);
					assertThat(context).hasBean("testAuthorizedClientRepository");
				});
	}

	@Test
	public void autoConfigurationConditionalOnClassFlux() {
		assertWhenClassNotPresent(Flux.class);
	}

	@Test
	public void autoConfigurationConditionalOnClassEnableWebFluxSecurity() {
		assertWhenClassNotPresent(EnableWebFluxSecurity.class);
	}

	@Test
	public void autoConfigurationConditionalOnClassClientRegistration() {
		assertWhenClassNotPresent(ClientRegistration.class);
	}

	private void assertWhenClassNotPresent(Class<?> classToFilter) {
		FilteredClassLoader classLoader = new FilteredClassLoader(classToFilter);
		this.contextRunner.withClassLoader(classLoader)
				.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
						REGISTRATION_PREFIX + ".foo.client-secret=secret",
						REGISTRATION_PREFIX + ".foo.provider=github")
				.run((context) -> assertThat(context)
						.doesNotHaveBean(ReactiveOAuth2ClientAutoConfiguration.class));
	}

	@Configuration
	static class ReactiveClientRepositoryConfiguration {

		@Bean
		public ReactiveClientRegistrationRepository clientRegistrationRepository() {
			List<ClientRegistration> registrations = new ArrayList<>();
			registrations.add(getClientRegistration("first", "http://user-info-uri.com"));
			registrations.add(getClientRegistration("second", "http://other-user-info"));
			return new InMemoryReactiveClientRegistrationRepository(registrations);
		}

		private ClientRegistration getClientRegistration(String id, String userInfoUri) {
			ClientRegistration.Builder builder = ClientRegistration
					.withRegistrationId(id);
			builder.clientName("foo").clientId("foo").clientAuthenticationMethod(
					org.springframework.security.oauth2.core.ClientAuthenticationMethod.BASIC)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
					.scope("read").clientSecret("secret")
					.redirectUriTemplate("http://redirect-uri.com")
					.authorizationUri("http://authorization-uri.com")
					.tokenUri("http://token-uri.com").userInfoUri(userInfoUri)
					.userNameAttributeName("login");
			return builder.build();
		}

	}

	@Configuration
	@Import(ReactiveClientRepositoryConfiguration.class)
	static class ReactiveOAuth2AuthorizedClientServiceConfiguration {

		@Bean
		public ReactiveOAuth2AuthorizedClientService testAuthorizedClientService(
				ReactiveClientRegistrationRepository clientRegistrationRepository) {
			return new InMemoryReactiveOAuth2AuthorizedClientService(
					clientRegistrationRepository);
		}

	}

	@Configuration
	@Import(ReactiveOAuth2AuthorizedClientServiceConfiguration.class)
	static class ReactiveOAuth2AuthorizedClientRepositoryConfiguration {

		@Bean
		public ServerOAuth2AuthorizedClientRepository testAuthorizedClientRepository(
				ReactiveOAuth2AuthorizedClientService authorizedClientService) {
			return new AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository(
					authorizedClientService);
		}

	}

}
