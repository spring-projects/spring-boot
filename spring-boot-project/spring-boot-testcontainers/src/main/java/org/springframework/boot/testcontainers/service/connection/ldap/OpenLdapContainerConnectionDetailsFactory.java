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

package org.springframework.boot.testcontainers.service.connection.ldap;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.ldap.LdapConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link LdapConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link GenericContainer} using
 * the {@code "osixia/openldap"} image.
 *
 * @author Philipp Kessler
 */
class OpenLdapContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, LdapConnectionDetails> {

	OpenLdapContainerConnectionDetailsFactory() {
		super("osixia/openldap");
	}

	@Override
	protected LdapConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new OpenLdapContainerConnectionDetails(source);
	}

	private static final class OpenLdapContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements LdapConnectionDetails {

		private OpenLdapContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		@Override
		public String[] getUrls() {
			Map<String, String> env = getContainer().getEnvMap();
			boolean usesTls = Boolean.parseBoolean(env.getOrDefault("LDAP_TLS", "true"));
			String ldapPort = usesTls ? env.getOrDefault("LDAPS_PORT", "636") : env.getOrDefault("LDAP_PORT", "389");
			return new String[] { "%s://%s:%d".formatted(usesTls ? "ldaps" : "ldap", getContainer().getHost(),
					getContainer().getMappedPort(Integer.parseInt(ldapPort))) };
		}

		@Override
		public String getBase() {
			Map<String, String> env = getContainer().getEnvMap();
			if (env.containsKey("LDAP_BASE_DN")) {
				return env.get("LDAP_BASE_DN");
			}
			return Arrays.stream(env.getOrDefault("LDAP_DOMAIN", "example.org").split("\\."))
				.map("dc=%s"::formatted)
				.collect(Collectors.joining(","));
		}

		@Override
		public String getUsername() {
			return "cn=admin,%s".formatted(getBase());
		}

		@Override
		public String getPassword() {
			return getContainer().getEnvMap().getOrDefault("LDAP_ADMIN_PASSWORD", "admin");
		}

	}

}
