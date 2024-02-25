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

	/**
     * Constructs a new OpenLdapContainerConnectionDetailsFactory with the specified image name.
     * 
     * @param imageName the name of the Docker image to use for the OpenLDAP container
     */
    OpenLdapContainerConnectionDetailsFactory() {
		super("osixia/openldap");
	}

	/**
     * Returns the LDAP connection details for the specified container connection source.
     *
     * @param source the container connection source
     * @return the LDAP connection details
     */
    @Override
	protected LdapConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new OpenLdapContainerConnectionDetails(source);
	}

	/**
     * OpenLdapContainerConnectionDetails class.
     */
    private static final class OpenLdapContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements LdapConnectionDetails {

		/**
         * Constructs a new OpenLdapContainerConnectionDetails object with the specified ContainerConnectionSource.
         *
         * @param source the ContainerConnectionSource used to create the OpenLdapContainerConnectionDetails object
         */
        private OpenLdapContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		/**
         * Returns an array of URLs for connecting to the LDAP server.
         * The URLs are generated based on the environment variables of the container.
         * If the LDAP_TLS environment variable is set to true, the URLs will use LDAPS protocol and the LDAPS_PORT environment variable.
         * Otherwise, the URLs will use LDAP protocol and the LDAP_PORT environment variable.
         * The host and port are obtained from the OpenLdapContainer instance.
         *
         * @return an array of URLs for connecting to the LDAP server
         */
        @Override
		public String[] getUrls() {
			Map<String, String> env = getContainer().getEnvMap();
			boolean usesTls = Boolean.parseBoolean(env.getOrDefault("LDAP_TLS", "true"));
			String ldapPort = usesTls ? env.getOrDefault("LDAPS_PORT", "636") : env.getOrDefault("LDAP_PORT", "389");
			return new String[] { "%s://%s:%d".formatted(usesTls ? "ldaps" : "ldap", getContainer().getHost(),
					getContainer().getMappedPort(Integer.parseInt(ldapPort))) };
		}

		/**
         * Returns the base DN (Distinguished Name) for the LDAP connection.
         * 
         * @return the base DN for the LDAP connection
         */
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

		/**
         * Returns the username for the OpenLdapContainerConnectionDetails.
         * The username is formatted as "cn=admin,%s", where %s is replaced with the base.
         *
         * @return the username for the OpenLdapContainerConnectionDetails
         */
        @Override
		public String getUsername() {
			return "cn=admin,%s".formatted(getBase());
		}

		/**
         * Returns the password for the LDAP admin.
         * If the "LDAP_ADMIN_PASSWORD" environment variable is set, it will be returned.
         * Otherwise, the default password "admin" will be returned.
         *
         * @return the password for the LDAP admin
         */
        @Override
		public String getPassword() {
			return getContainer().getEnvMap().getOrDefault("LDAP_ADMIN_PASSWORD", "admin");
		}

	}

}
