/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.oauth2.client;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientConnectionDetails;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientConnectionDetails.Provider;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientConnectionDetails.Registration;
import org.springframework.boot.docker.compose.service.connection.test.AbstractDockerComposeIntegrationTests;
import org.springframework.boot.testsupport.testcontainers.DockerImageNames;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link KeycloakDockerComposeConnectionDetailsFactory}.
 *
 * @author Philipp Kessler
 */
class KeycloakDockerComposeConnectionDetailsFactoryTests extends AbstractDockerComposeIntegrationTests {

	KeycloakDockerComposeConnectionDetailsFactoryTests() {
		super("keycloak-compose.yaml", DockerImageNames.keycloak());
	}

	@Test
	void runCreatesConnectionDetails() {
		OAuth2ClientConnectionDetails connectionDetails = run(OAuth2ClientConnectionDetails.class);
		Map<String, Registration> registrations = connectionDetails.getRegistrations();
		Map<String, Provider> providers = connectionDetails.getProviders();
		assertThat(registrations).isNotNull();
		assertThat(providers).isNotNull();
		assertThat(registrations).containsKey("keycloak-client");
		assertThat(providers).containsKey("keycloak");
		Registration registration = registrations.get("keycloak-client");
		assertThat(registration.getProvider()).isEqualTo("keycloak");
		assertThat(registration.getClientSecret()).isEqualTo("secret");
		assertThat(registration.getScopes()).containsExactly("openid", "some_scope");
		Provider provider = providers.get("keycloak");
		assertThat(provider.getIssuerUri()).startsWith("http://");
		assertThat(provider.getIssuerUri()).endsWith("/realms/KeycloakRealm");
	}

}
