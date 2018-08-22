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

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveOAuth2ClientAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class ReactiveOAuth2ClientAutoConfigurationTests {

	private ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(ReactiveOAuth2ClientAutoConfiguration.class));

	private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration.login";

	@Test
	public void autoConfigurationShouldImportConfigurations() {
		this.contextRunner.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
				REGISTRATION_PREFIX + ".foo.client-secret=secret",
				REGISTRATION_PREFIX + ".foo.provider=github").run((context) -> {
					assertThat(context).hasSingleBean(
							ReactiveOAuth2ClientRegistrationRepositoryConfiguration.class);
					assertThat(context)
							.hasSingleBean(ReactiveOAuth2WebSecurityConfiguration.class);
				});
	}

	@Test
	public void autoConfigurationConditionalOnClassEnableWebFluxSecurity() {
		FilteredClassLoader classLoader = new FilteredClassLoader(
				EnableWebFluxSecurity.class);
		this.contextRunner.withClassLoader(classLoader)
				.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
						REGISTRATION_PREFIX + ".foo.client-secret=secret",
						REGISTRATION_PREFIX + ".foo.provider=github")
				.run((context) -> {
					assertThat(context).doesNotHaveBean(
							ReactiveOAuth2ClientRegistrationRepositoryConfiguration.class);
					assertThat(context).doesNotHaveBean(
							ReactiveOAuth2WebSecurityConfiguration.class);
				});
	}

	@Test
	public void autoConfigurationConditionalOnClassClientRegistration() {
		FilteredClassLoader classLoader = new FilteredClassLoader(
				ClientRegistration.class);
		this.contextRunner.withClassLoader(classLoader)
				.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
						REGISTRATION_PREFIX + ".foo.client-secret=secret",
						REGISTRATION_PREFIX + ".foo.provider=github")
				.run((context) -> {
					assertThat(context).doesNotHaveBean(
							ReactiveOAuth2ClientRegistrationRepositoryConfiguration.class);
					assertThat(context).doesNotHaveBean(
							ReactiveOAuth2WebSecurityConfiguration.class);
				});
	}

	@Test
	public void autoConfigurationConditionalOnReactiveWebApplication() {
		WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations
						.of(ReactiveOAuth2ClientAutoConfiguration.class));
		contextRunner.withPropertyValues(REGISTRATION_PREFIX + ".foo.client-id=abcd",
				REGISTRATION_PREFIX + ".foo.client-secret=secret",
				REGISTRATION_PREFIX + ".foo.provider=github").run((context) -> {
					assertThat(context).doesNotHaveBean(
							ReactiveOAuth2ClientRegistrationRepositoryConfiguration.class);
					assertThat(context).doesNotHaveBean(
							ReactiveOAuth2WebSecurityConfiguration.class);
				});
	}

}
