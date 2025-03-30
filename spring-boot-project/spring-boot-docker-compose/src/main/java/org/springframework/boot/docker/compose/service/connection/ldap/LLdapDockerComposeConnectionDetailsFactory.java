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

package org.springframework.boot.docker.compose.service.connection.ldap;

import java.util.Map;

import org.springframework.boot.autoconfigure.ldap.LdapConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link LdapConnectionDetails}
 * for an {@code ldap} service.
 *
 * @author Eddú Meléndez
 */
class LLdapDockerComposeConnectionDetailsFactory extends DockerComposeConnectionDetailsFactory<LdapConnectionDetails> {

	LLdapDockerComposeConnectionDetailsFactory() {
		super("lldap/lldap");
	}

	@Override
	protected LdapConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new LLdapDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link LdapConnectionDetails} backed by an {@code lldap} {@link RunningService}.
	 */
	static class LLdapDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements LdapConnectionDetails {

		private final String[] urls;

		private final String base;

		private final String username;

		private final String password;

		LLdapDockerComposeConnectionDetails(RunningService service) {
			super(service);
			Map<String, String> env = service.env();
			boolean usesTls = Boolean.parseBoolean(env.getOrDefault("LLDAP_LDAPS_OPTIONS__ENABLED", "false"));
			String ldapPort = usesTls ? env.getOrDefault("LLDAP_LDAPS_OPTIONS__PORT", "6360")
					: env.getOrDefault("LLDAP_LDAP_PORT", "3890");
			this.urls = new String[] { "%s://%s:%d".formatted(usesTls ? "ldaps" : "ldap", service.host(),
					service.ports().get(Integer.parseInt(ldapPort))) };
			this.base = env.getOrDefault("LLDAP_LDAP_BASE_DN", "dc=example,dc=com");
			this.password = env.getOrDefault("LLDAP_LDAP_USER_PASS", "password");
			this.username = "cn=admin,ou=people,%s".formatted(this.base);
		}

		@Override
		public String[] getUrls() {
			return this.urls;
		}

		@Override
		public String getBase() {
			return this.base;
		}

		@Override
		public String getUsername() {
			return this.username;
		}

		@Override
		public String getPassword() {
			return this.password;
		}

	}

}
