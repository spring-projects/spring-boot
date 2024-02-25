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

package org.springframework.boot.docker.compose.service.connection.ldap;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.ldap.LdapConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link LdapConnectionDetails}
 * for an {@code ldap} service.
 *
 * @author Philipp Kessler
 */
class OpenLdapDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<LdapConnectionDetails> {

	/**
	 * Constructs a new OpenLdapDockerComposeConnectionDetailsFactory object.
	 *
	 * This constructor initializes the OpenLdapDockerComposeConnectionDetailsFactory
	 * object with the specified Docker image name "osixia/openldap".
	 *
	 * @since 1.0
	 */
	protected OpenLdapDockerComposeConnectionDetailsFactory() {
		super("osixia/openldap");
	}

	/**
	 * Retrieves the connection details for connecting to an OpenLDAP server running in a
	 * Docker Compose environment.
	 * @param source the DockerComposeConnectionSource object containing the details of
	 * the running service
	 * @return the LdapConnectionDetails object representing the connection details
	 */
	@Override
	protected LdapConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OpenLdapDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link LdapConnectionDetails} backed by an {@code openldap} {@link RunningService}.
	 */
	static class OpenLdapDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements LdapConnectionDetails {

		private final String[] urls;

		private final String base;

		private final String username;

		private final String password;

		/**
		 * Constructs a new OpenLdapDockerComposeConnectionDetails object with the
		 * provided RunningService.
		 * @param service the RunningService object representing the running LDAP service
		 */
		OpenLdapDockerComposeConnectionDetails(RunningService service) {
			super(service);
			Map<String, String> env = service.env();
			boolean usesTls = Boolean.parseBoolean(env.getOrDefault("LDAP_TLS", "true"));
			String ldapPort = usesTls ? env.getOrDefault("LDAPS_PORT", "636") : env.getOrDefault("LDAP_PORT", "389");
			this.urls = new String[] { "%s://%s:%d".formatted(usesTls ? "ldaps" : "ldap", service.host(),
					service.ports().get(Integer.parseInt(ldapPort))) };
			if (env.containsKey("LDAP_BASE_DN")) {
				this.base = env.get("LDAP_BASE_DN");
			}
			else {
				this.base = Arrays.stream(env.getOrDefault("LDAP_DOMAIN", "example.org").split("\\."))
					.map("dc=%s"::formatted)
					.collect(Collectors.joining(","));
			}
			this.password = env.getOrDefault("LDAP_ADMIN_PASSWORD", "admin");
			this.username = "cn=admin,%s".formatted(this.base);
		}

		/**
		 * Returns an array of URLs.
		 * @return the array of URLs
		 */
		@Override
		public String[] getUrls() {
			return this.urls;
		}

		/**
		 * Returns the base DN (Distinguished Name) for the LDAP connection.
		 * @return the base DN for the LDAP connection
		 */
		@Override
		public String getBase() {
			return this.base;
		}

		/**
		 * Returns the username associated with this
		 * OpenLdapDockerComposeConnectionDetails object.
		 * @return the username
		 */
		@Override
		public String getUsername() {
			return this.username;
		}

		/**
		 * Returns the password associated with the
		 * OpenLdapDockerComposeConnectionDetails.
		 * @return the password associated with the OpenLdapDockerComposeConnectionDetails
		 */
		@Override
		public String getPassword() {
			return this.password;
		}

	}

}
