/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.testcontainers;

import org.jspecify.annotations.Nullable;
import org.testcontainers.ldap.LLdapContainer;

import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link LdapConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link LLdapContainer}.
 *
 * @author Eddú Meléndez
 */
class LLdapContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<LLdapContainer, LdapConnectionDetails> {

	@Override
	protected LdapConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<LLdapContainer> source) {
		return new LLdapContainerConnectionDetails(source);
	}

	private static final class LLdapContainerConnectionDetails extends ContainerConnectionDetails<LLdapContainer>
			implements LdapConnectionDetails {

		private LLdapContainerConnectionDetails(ContainerConnectionSource<LLdapContainer> source) {
			super(source);
		}

		@Override
		public String[] getUrls() {
			return new String[] { getContainer().getLdapUrl() };
		}

		@Override
		public String getBase() {
			return getContainer().getBaseDn();
		}

		@Override
		public String getUsername() {
			return getContainer().getUser();
		}

		@Override
		public String getPassword() {
			return getContainer().getPassword();
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
