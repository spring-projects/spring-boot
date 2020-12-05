/*
 * Copyright 2012-2019 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
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
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
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
 * Tests for {@link ReactiveOAuth2ClientAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class ReactiveOAuth2ClientAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations
			.of(ReactiveOAuth2ClientAutoConfiguration.class, ReactiveSecurityAutoConfiguration.class));

	private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration";

	@Test
	void autoConfigurationShouldBackOffForServletEnvironments() {
		new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(ReactiveOAuth2ClientAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(ReactiveOAuth2ClientAutoConfiguration.class));
	}

	@Test
	void clientRegistrationRepositoryBeanShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class));
	}

	@Test
	void clientRegistrationRepositoryBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner
				.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
						REGISTRATION_PREFIX + ".foo.client-secret=secret", REGISTRATION_PREFIX + ".foo.provider=github")
				.run((context) -> {
					ReactiveClientRegistrationRepository repository = context
							.getBean(ReactiveClientRegistrationRepository.class);
					ClientRegistration registration = repository.findByRegistrationId("foo")
							.block(Duration.ofSeconds(30));
					assertThat(registration).isNotNull();
					assertThat(registration.getClientSecret()).isEqualTo("secret");
				});
	}

	@Test
	void authorizedClientServiceAndRepositoryBeansAreConditionalOnClientRegistrationRepository() {
		this.contextRunner.run((context) -> {
			assertThat(context).doesNotHaveBean(ReactiveOAuth2AuthorizedClientService.class);
			assertThat(context).doesNotHaveBean(ServerOAuth2AuthorizedClientRepository.class);
		});
	}

	@Test
	void configurationRegistersAuthorizedClientServiceAndRepositoryBeans() {
		this.contextRunner.withUserConfiguration(ReactiveClientRepositoryConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(InMemoryReactiveOAuth2AuthorizedClientService.class);
			assertThat(context).hasSingleBean(AuthenticatedPrincipalServerOAuth2AuthorizedClientRepository.class);
		});
	}

	@Test
	void authorizedClientServiceBeanIsConditionalOnMissingBean() {
		this.contextRunner.withUserConfiguration(ReactiveOAuth2AuthorizedClientRepositoryConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(ReactiveOAuth2AuthorizedClientService.class);
					assertThat(context).hasBean("testAuthorizedClientService");
				});
	}

	@Test
	void authorizedClientRepositoryBeanIsConditionalOnAuthorizedClientService() {
		this.contextRunner
				.run((context) -> assertThat(context).doesNotHaveBean(ServerOAuth2AuthorizedClientRepository.class));
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
	void securityWebFilterChainBeanConditionalOnWebApplication() {
		this.contextRunner.withUserConfiguration(ReactiveOAuth2AuthorizedClientRepositoryConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(SecurityWebFilterChain.class));
	}

	@Test
	void configurationRegistersSecurityWebFilterChainBean() { // gh-17949
		new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(ReactiveOAuth2ClientAutoConfiguration.class))
				.withUserConfiguration(ReactiveOAuth2AuthorizedClientServiceConfiguration.class,
						ServerHttpSecurityConfiguration.class)
				.run((context) -> {
					assertThat(hasFilter(context, OAuth2LoginAuthenticationWebFilter.class)).isTrue();
					assertThat(hasFilter(context, OAuth2AuthorizationCodeGrantWebFilter.class)).isTrue();
				});
	}

	@Test
	void autoConfigurationConditionalOnClassFlux() {
		assertWhenClassNotPresent(Flux.class);
	}

	@Test
	void autoConfigurationConditionalOnClassEnableWebFluxSecurity() {
		assertWhenClassNotPresent(EnableWebFluxSecurity.class);
	}

	@Test
	void autoConfigurationConditionalOnClassClientRegistration() {
		assertWhenClassNotPresent(ClientRegistration.class);
	}

	private void assertWhenClassNotPresent(Class<?> classToFilter) {
		FilteredClassLoader classLoader = new FilteredClassLoader(classToFilter);
		this.contextRunner.withClassLoader(classLoader)
				.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
						REGISTRATION_PREFIX + ".foo.client-secret=secret", REGISTRATION_PREFIX + ".foo.provider=github")
				.run((context) -> assertThat(context).doesNotHaveBean(ReactiveOAuth2ClientAutoConfiguration.class));
	}

	@SuppressWarnings("unchecked")
	private boolean hasFilter(AssertableReactiveWebApplicationContext context, Class<? extends WebFilter> filter) {
		SecurityWebFilterChain filterChain = (SecurityWebFilterChain) context
				.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<WebFilter> filters = (List<WebFilter>) ReflectionTestUtils.getField(filterChain, "filters");
		return filters.stream().anyMatch(filter::isInstance);
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
			builder.clientName("foo").clientId("foo")
					.clientAuthenticationMethod(
							org.springframework.security.oauth2.core.ClientAuthenticationMethod.BASIC)
					.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).scope("read")
					.clientSecret("secret").redirectUriTemplate("https://redirect-uri.com")
					.authorizationUri("https://authorization-uri.com").tokenUri("https://token-uri.com")
					.userInfoUri(userInfoUri).userNameAttributeName("login");
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(ReactiveClientRepositoryConfiguration.class)
	static class ReactiveOAuth2AuthorizedClientServiceConfiguration {

		@Bean
		ReactiveOAuth2AuthorizedClientService testAuthorizedClientService(
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
	static class ServerHttpSecurityConfiguration {

		@Bean
		ServerHttpSecurity http() {
			TestServerHttpSecurity httpSecurity = new TestServerHttpSecurity();
			return httpSecurity;
		}

		static class TestServerHttpSecurity extends ServerHttpSecurity implements ApplicationContextAware {

			@Override
			public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
				super.setApplicationContext(applicationContext);
			}

		}

	}

}
