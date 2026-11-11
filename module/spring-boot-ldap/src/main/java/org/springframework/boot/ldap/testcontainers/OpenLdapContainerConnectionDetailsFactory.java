/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.testcontainers;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
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

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
