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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveOAuth2WebSecurityConfiguration}.
 *
 * @author Madhura Bhave
 */
public class ReactiveOAuth2WebSecurityConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	public void authorizedClientServiceBeanIsConditionalOnClientRegistrationRepository() {
		this.contextRunner
				.withUserConfiguration(ReactiveOAuth2WebSecurityConfiguration.class)
				.run((context) -> assertThat(context)
						.doesNotHaveBean(ReactiveOAuth2AuthorizedClientService.class));
	}

	@Test
	public void configurationRegistersAuthorizedClientServiceBean() {
		this.contextRunner
				.withUserConfiguration(ReactiveClientRepositoryConfiguration.class,
						ReactiveOAuth2WebSecurityConfiguration.class)
				.run((context) -> assertThat(context)
						.hasSingleBean(ReactiveOAuth2AuthorizedClientService.class));
	}

	@Test
	public void authorizedClientServiceBeanIsConditionalOnMissingBean() {
		this.contextRunner
				.withUserConfiguration(OAuth2AuthorizedClientServiceConfiguration.class,
						ReactiveOAuth2WebSecurityConfiguration.class)
				.run((context) -> {
					assertThat(context)
							.hasSingleBean(ReactiveOAuth2AuthorizedClientService.class);
					assertThat(context).hasBean("testAuthorizedClientService");
				});
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
	static class OAuth2AuthorizedClientServiceConfiguration {

		@Bean
		public ReactiveOAuth2AuthorizedClientService testAuthorizedClientService(
				ReactiveClientRegistrationRepository clientRegistrationRepository) {
			return new InMemoryReactiveOAuth2AuthorizedClientService(
					clientRegistrationRepository);
		}

	}

}
