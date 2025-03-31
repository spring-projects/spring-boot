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

package org.springframework.boot.security.oauth2.client.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OAuth2ClientAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
class OAuth2ClientAutoConfigurationTests {

	private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OAuth2ClientAutoConfiguration.class));

	@Test
	void beansShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ClientRegistrationRepository.class)
			.doesNotHaveBean(OAuth2AuthorizedClientService.class));
	}

	@Test
	void beansAreCreatedWhenPropertiesPresent() {
		this.contextRunner
			.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
					REGISTRATION_PREFIX + ".foo.client-secret=secret", REGISTRATION_PREFIX + ".foo.provider=github")
			.run((context) -> {
				assertThat(context).hasSingleBean(ClientRegistrationRepository.class);
				assertThat(context).hasSingleBean(OAuth2AuthorizedClientService.class);
				ClientRegistrationRepository repository = context.getBean(ClientRegistrationRepository.class);
				ClientRegistration registration = repository.findByRegistrationId("foo");
				assertThat(registration).isNotNull();
				assertThat(registration.getClientSecret()).isEqualTo("secret");
			});
	}

	@Test
	void clientServiceBeanIsConditionalOnMissingBean() {
		this.contextRunner
			.withBean("testAuthorizedClientService", OAuth2AuthorizedClientService.class,
					() -> mock(OAuth2AuthorizedClientService.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(OAuth2AuthorizedClientService.class);
				assertThat(context).hasBean("testAuthorizedClientService");
			});
	}

	@Test
	void clientServiceBeanIsCreatedWithUserDefinedClientRegistrationRepository() {
		this.contextRunner
			.withBean(ClientRegistrationRepository.class,
					() -> new InMemoryClientRegistrationRepository(getClientRegistration("test", "test")))
			.run((context) -> assertThat(context).hasSingleBean(OAuth2AuthorizedClientService.class));
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
