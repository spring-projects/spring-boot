/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure.reactive;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
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
				ReactiveWebSecurityAutoConfiguration.class));

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
