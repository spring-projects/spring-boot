/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.testcontainers;

import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link JdbcConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link JdbcDatabaseContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class JdbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<JdbcDatabaseContainer<?>, JdbcConnectionDetails> {

	@Override
	protected JdbcConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
		return new JdbcContainerConnectionDetails(source);
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class JdbcContainerConnectionDetails
			extends ContainerConnectionDetails<JdbcDatabaseContainer<?>> implements JdbcConnectionDetails {

		private JdbcContainerConnectionDetails(ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
			super(source);
		}

		@Override
		public String getUsername() {
			return getContainer().getUsername();
		}

		@Override
		public String getPassword() {
			return getContainer().getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return getContainer().getJdbcUrl();
		}

		@Override
		public String getDriverClassName() {
			return getContainer().getDriverClassName();
		}

	}

}
