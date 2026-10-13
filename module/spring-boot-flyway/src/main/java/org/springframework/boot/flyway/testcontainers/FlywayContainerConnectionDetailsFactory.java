/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.flyway.testcontainers;

import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.boot.flyway.autoconfigure.FlywayConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link FlywayConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link JdbcDatabaseContainer}.
 *
 * @author Andy Wilkinson
 */
class FlywayContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<JdbcDatabaseContainer<?>, FlywayConnectionDetails> {

	@Override
	protected FlywayConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
		return new FlywayContainerConnectionDetails(source);
	}

	/**
	 * {@link FlywayConnectionDetails} backed by a {@link JdbcDatabaseContainer}.
	 */
	private static final class FlywayContainerConnectionDetails
			extends ContainerConnectionDetails<JdbcDatabaseContainer<?>> implements FlywayConnectionDetails {

		private FlywayContainerConnectionDetails(ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
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
