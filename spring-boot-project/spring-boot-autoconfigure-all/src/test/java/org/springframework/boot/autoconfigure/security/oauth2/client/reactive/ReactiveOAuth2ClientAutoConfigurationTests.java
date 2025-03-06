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

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveOAuth2ClientAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class ReactiveOAuth2ClientAutoConfigurationTests {

	private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ReactiveOAuth2ClientAutoConfiguration.class,
				ReactiveSecurityAutoConfiguration.class));

	@Test
	void autoConfigurationShouldBackOffForServletEnvironments() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ReactiveOAuth2ClientAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveOAuth2ClientAutoConfiguration.class));
	}

	@Test
	void beansShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner
			.run((context) -> assertThat(context).doesNotHaveBean(ReactiveClientRegistrationRepository.class)
				.doesNotHaveBean(ReactiveOAuth2AuthorizedClientService.class));
	}

	@Test
	void beansAreCreatedWhenPropertiesPresent() {
		this.contextRunner
			.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
					REGISTRATION_PREFIX + ".foo.client-secret=secret", REGISTRATION_PREFIX + ".foo.provider=github")
			.run((context) -> {
				assertThat(context).hasSingleBean(ReactiveClientRegistrationRepository.class);
				assertThat(context).hasSingleBean(ReactiveOAuth2AuthorizedClientService.class);
				ReactiveClientRegistrationRepository repository = context
					.getBean(ReactiveClientRegistrationRepository.class);
				ClientRegistration registration = repository.findByRegistrationId("foo").block(Duration.ofSeconds(30));
				assertThat(registration).isNotNull();
				assertThat(registration.getClientSecret()).isEqualTo("secret");
			});
	}

	@Test
	void clientServiceBeanIsConditionalOnMissingBean() {
		this.contextRunner
			.withBean("testAuthorizedClientService", ReactiveOAuth2AuthorizedClientService.class,
					() -> mock(ReactiveOAuth2AuthorizedClientService.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(ReactiveOAuth2AuthorizedClientService.class);
				assertThat(context).hasBean("testAuthorizedClientService");
			});
	}

	@Test
	void clientServiceBeanIsCreatedWithUserDefinedClientRegistrationRepository() {
		this.contextRunner
			.withBean(InMemoryReactiveClientRegistrationRepository.class,
					() -> new InMemoryReactiveClientRegistrationRepository(getClientRegistration("test", "test")))
			.run((context) -> assertThat(context).hasSingleBean(ReactiveOAuth2AuthorizedClientService.class));
	}

	@Test
	void autoConfigurationConditionalOnClassFlux() {
		assertWhenClassNotPresent(Flux.class);
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
