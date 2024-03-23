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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link OAuth2ClientConnectionDetails} for a {@code keycloak} service.
 *
 * @author Philipp Kessler
 */
class KeycloakDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<OAuth2ClientConnectionDetails> {

	KeycloakDockerComposeConnectionDetailsFactory() {
		super("keycloak/keycloak");
	}

	@Override
	protected OAuth2ClientConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new KeycloakDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link RedisConnectionDetails} backed by a {@code redis} {@link RunningService}.
	 */
	static class KeycloakDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements OAuth2ClientConnectionDetails {

		private static final String KC_HOSTNAME = "KC_HOSTNAME";

		private static final String KC_HOSTNAME_DEFAULT = "0.0.0.0";

		private static final String KC_REALM_DEFAULT = "master";

		private static final String KC_HTTPS_CERTIFICATE_FILE = "KC_HTTPS_CERTIFICATE_FILE";

		private static final Integer KC_PORT_HTTP_DEFAULT = 8080;

		private static final Integer KC_PORT_HTTPS_DEFAULT = 8443;

		private static final String CLIENT_SCOPES_DEFAULT = "openid";

		private static final String REGISTRATION_ID_DEFAULT = "keycloak";

		private static final String REALM_LABEL = "org.springframework.boot.security.oauth2.client.keycloak.realm";

		private static final String CLIENT_ID_LABEL = "org.springframework.boot.security.oauth2.client.id";

		private static final String CLIENT_SECRET_LABEL = "org.springframework.boot.security.oauth2.client.secret";

		private static final String CLIENT_SCOPES_LABEL = "org.springframework.boot.security.oauth2.client.scopes";

		public static final String PROVIDER_DEFAULT = "keycloak";

		private final Map<String, Registration> registrations;

		private final Map<String, Provider> providers;

		KeycloakDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.registrations = new HashMap<>();
			this.providers = new HashMap<>();
			Map<String, String> env = service.env();
			String host = env.getOrDefault(KC_HOSTNAME, KC_HOSTNAME_DEFAULT);
			String httpsCertFile = env.getOrDefault(KC_HTTPS_CERTIFICATE_FILE, null);
			boolean isHttpsEnabled = httpsCertFile != null && httpsCertFile.isEmpty() && httpsCertFile.isBlank();
			Integer port = isHttpsEnabled ? KC_PORT_HTTPS_DEFAULT : KC_PORT_HTTP_DEFAULT;
			Integer actualPort = service.ports().get(port);
			Map<String, String> labels = service.labels();
			String realm = labels.getOrDefault(REALM_LABEL, KC_REALM_DEFAULT);
			Provider provider = new Provider() {
				@Override
				public String getIssuerUri() {
					return "%s://%s:%s/realms/%s".formatted(isHttpsEnabled ? "https" : "http", host, actualPort, realm);
				}
			};
			this.providers.put(PROVIDER_DEFAULT, provider);
			String registrationId = labels.getOrDefault(CLIENT_ID_LABEL, REGISTRATION_ID_DEFAULT);
			String clientSecret = labels.getOrDefault(CLIENT_SECRET_LABEL, null);
			Set<String> scopes = Arrays
				.stream(labels.getOrDefault(CLIENT_SCOPES_LABEL, CLIENT_SCOPES_DEFAULT).split(","))
				.collect(Collectors.toSet());
			Registration registration = new Registration() {

				@Override
				public String getProvider() {
					return PROVIDER_DEFAULT;
				}

				@Override
				public String getClientId() {
					return registrationId;
				}

				@Override
				public String getClientSecret() {
					return clientSecret;
				}

				@Override
				public String getClientAuthenticationMethod() {
					return "client_secret_basic";
				}

				@Override
				public String getAuthorizationGrantType() {
					return "authorization_code";
				}

				@Override
				public String getRedirectUri() {
					return null;
				}

				@Override
				public Set<String> getScopes() {
					return scopes;
				}

				@Override
				public String getClientName() {
					return registrationId;
				}
			};
			this.registrations.put(registrationId, registration);
		}

		@Override
		public Map<String, Registration> getRegistrations() {
			return this.registrations;
		}

		@Override
		public Map<String, Provider> getProviders() {
			return this.providers;
		}

	}

}
