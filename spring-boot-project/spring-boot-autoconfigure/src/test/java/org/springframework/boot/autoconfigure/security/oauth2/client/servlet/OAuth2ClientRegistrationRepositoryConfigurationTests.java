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

package org.springframework.boot.autoconfigure.security.oauth2.client.servlet;

import org.junit.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2ClientRegistrationRepositoryConfiguration}.
 *
 * @author Madhura Bhave
 */
public class OAuth2ClientRegistrationRepositoryConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration";

	@Test
	public void clientRegistrationRepositoryBeanShouldNotBeCreatedWhenPropertiesAbsent() {
		this.contextRunner
				.withUserConfiguration(
						OAuth2ClientRegistrationRepositoryConfiguration.class)
				.run((context) -> assertThat(context)
						.doesNotHaveBean(ClientRegistrationRepository.class));
	}

	@Test
	public void clientRegistrationRepositoryBeanShouldBeCreatedWhenPropertiesPresent() {
		this.contextRunner
				.withUserConfiguration(
						OAuth2ClientRegistrationRepositoryConfiguration.class)
				.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
						REGISTRATION_PREFIX + ".foo.client-secret=secret",
						REGISTRATION_PREFIX + ".foo.provider=github")
				.run((context) -> {
					ClientRegistrationRepository repository = context
							.getBean(ClientRegistrationRepository.class);
					ClientRegistration registration = repository
							.findByRegistrationId("foo");
					assertThat(registration).isNotNull();
					assertThat(registration.getClientSecret()).isEqualTo("secret");
				});
	}

}
