/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.testcontainers;

import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.boot.liquibase.autoconfigure.LiquibaseConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link LiquibaseConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link JdbcDatabaseContainer}.
 *
 * @author Andy Wilkinson
 */
class LiquibaseContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<JdbcDatabaseContainer<?>, LiquibaseConnectionDetails> {

	@Override
	protected LiquibaseConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
		return new LiquibaseContainerConnectionDetails(source);
	}

	/**
	 * {@link LiquibaseConnectionDetails} backed by a {@link JdbcDatabaseContainer}.
	 */
	private static final class LiquibaseContainerConnectionDetails
			extends ContainerConnectionDetails<JdbcDatabaseContainer<?>> implements LiquibaseConnectionDetails {

		private LiquibaseContainerConnectionDetails(ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
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
