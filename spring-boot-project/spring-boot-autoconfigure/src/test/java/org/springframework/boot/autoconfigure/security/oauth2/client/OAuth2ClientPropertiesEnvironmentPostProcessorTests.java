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
package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2ClientPropertiesEnvironmentPostProcessor}.
 *
 * @author Madhura Bhave
 */
public class OAuth2ClientPropertiesEnvironmentPostProcessorTests {

	private OAuth2ClientPropertiesEnvironmentPostProcessor postProcessor = new OAuth2ClientPropertiesEnvironmentPostProcessor();

	private MockEnvironment environment;

	private static final String REGISTRATION_PREFIX = "spring.security.oauth2.client.registration.github-client.";

	private static final String ENVIRONMENT_REGISTRATION_PREFIX = "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB-CLIENT_";

	private static final String LOGIN_REGISTRATION_PREFIX = "spring.security.oauth2.client.registration.login.github-client.";

	@Before
	public void setup() {
		this.environment = new MockEnvironment();
	}

	@Test
	public void postProcessorWhenLegacyPropertiesShouldConvert() {
		Map<String, Object> properties = new HashMap<>();
		properties.put(REGISTRATION_PREFIX + "client-id", "my-client-id");
		properties.put(REGISTRATION_PREFIX + "client-secret", "my-client-secret");
		properties.put(REGISTRATION_PREFIX + "redirect-uri-template",
				"http://my-redirect-uri.com");
		properties.put(REGISTRATION_PREFIX + "provider", "github");
		properties.put(REGISTRATION_PREFIX + "scope", "user");
		properties.put(REGISTRATION_PREFIX + "client-name", "my-client-name");
		properties.put(REGISTRATION_PREFIX + "authorization-grant-type",
				"authorization_code");
		properties.put(REGISTRATION_PREFIX + "client-authentication-method", "FORM");
		MapPropertySource source = new MapPropertySource("test", properties);
		this.environment.getPropertySources().addFirst(source);
		this.postProcessor.postProcessEnvironment(this.environment, null);
		assertPropertyMigration();
	}

	@Test
	public void postProcessorDoesNotCopyMissingProperties() {
		Map<String, Object> properties = new HashMap<>();
		properties.put(REGISTRATION_PREFIX + "client-id", "my-client-id");
		MapPropertySource source = new MapPropertySource("test", properties);
		this.environment.getPropertySources().addFirst(source);
		this.postProcessor.postProcessEnvironment(this.environment, null);
		assertThat(this.environment.getProperty(LOGIN_REGISTRATION_PREFIX + "client-id"))
				.isEqualTo("my-client-id");
		assertThat(
				this.environment.getProperty(LOGIN_REGISTRATION_PREFIX + "client-secret"))
						.isNull();
	}

	@Test
	public void postProcessorWhenLegacyEnvironmentVariablesPropertiesShouldConvert() {
		Map<String, Object> properties = new HashMap<>();
		properties.put(ENVIRONMENT_REGISTRATION_PREFIX + "CLIENTID", "my-client-id");
		properties.put(ENVIRONMENT_REGISTRATION_PREFIX + "CLIENTSECRET",
				"my-client-secret");
		properties.put(ENVIRONMENT_REGISTRATION_PREFIX + "REDIRECTURITEMPLATE",
				"http://my-redirect-uri.com");
		properties.put(ENVIRONMENT_REGISTRATION_PREFIX + "PROVIDER", "github");
		properties.put(ENVIRONMENT_REGISTRATION_PREFIX + "SCOPE", "user");
		properties.put(ENVIRONMENT_REGISTRATION_PREFIX + "CLIENTNAME", "my-client-name");
		properties.put(ENVIRONMENT_REGISTRATION_PREFIX + "AUTHORIZATIONGRANTTYPE",
				"authorization_code");
		properties.put(ENVIRONMENT_REGISTRATION_PREFIX + "CLIENTAUTHENTICATIONMETHOD",
				"FORM");
		SystemEnvironmentPropertySource source = new SystemEnvironmentPropertySource(
				"test-" + StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
				properties);
		this.environment.getPropertySources().addFirst(source);
		this.postProcessor.postProcessEnvironment(this.environment, null);
		assertPropertyMigration();
	}

	@Test
	public void postProcessorWhenNewPropertiesShouldDoNothing() {
		Map<String, Object> properties = new HashMap<>();
		properties.put(LOGIN_REGISTRATION_PREFIX + "client-id", "my-client-id");
		properties.put(LOGIN_REGISTRATION_PREFIX + "client-secret", "my-client-secret");
		properties.put(LOGIN_REGISTRATION_PREFIX + "redirect-uri-template",
				"http://my-redirect-uri.com");
		properties.put(LOGIN_REGISTRATION_PREFIX + "provider", "github");
		properties.put(LOGIN_REGISTRATION_PREFIX + "scope", "user");
		properties.put(LOGIN_REGISTRATION_PREFIX + "client-name", "my-client-name");
		properties.put(LOGIN_REGISTRATION_PREFIX + "authorization-grant-type",
				"authorization_code");
		properties.put(LOGIN_REGISTRATION_PREFIX + "client-authentication-method",
				"FORM");
		MapPropertySource source = new MapPropertySource("test", properties);
		this.environment.getPropertySources().addFirst(source);
		MutablePropertySources propertySources = new MutablePropertySources(
				this.environment.getPropertySources());
		this.postProcessor.postProcessEnvironment(this.environment, null);
		assertPropertyMigration();
		assertThat(this.environment.getPropertySources())
				.containsExactlyElementsOf(propertySources);
	}

	private void assertPropertyMigration() {
		assertThat(this.environment.getProperty(LOGIN_REGISTRATION_PREFIX + "client-id"))
				.isEqualTo("my-client-id");
		assertThat(
				this.environment.getProperty(LOGIN_REGISTRATION_PREFIX + "client-secret"))
						.isEqualTo("my-client-secret");
		assertThat(this.environment
				.getProperty(LOGIN_REGISTRATION_PREFIX + "redirect-uri-template"))
						.isEqualTo("http://my-redirect-uri.com");
		assertThat(this.environment.getProperty(LOGIN_REGISTRATION_PREFIX + "provider"))
				.isEqualTo("github");
		assertThat(this.environment.getProperty(LOGIN_REGISTRATION_PREFIX + "scope"))
				.isEqualTo("user");
		assertThat(
				this.environment.getProperty(LOGIN_REGISTRATION_PREFIX + "client-name"))
						.isEqualTo("my-client-name");
		assertThat(this.environment
				.getProperty(LOGIN_REGISTRATION_PREFIX + "authorization-grant-type"))
						.isEqualTo("authorization_code");
		assertThat(this.environment
				.getProperty(LOGIN_REGISTRATION_PREFIX + "client-authentication-method"))
						.isEqualTo("FORM");
	}

}
