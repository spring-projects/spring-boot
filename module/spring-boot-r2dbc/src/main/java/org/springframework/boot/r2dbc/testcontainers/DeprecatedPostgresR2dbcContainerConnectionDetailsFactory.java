/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.testcontainers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PostgreSQLR2DBCDatabaseContainer;

import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link R2dbcConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link PostgreSQLContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
 * {@link PostgresR2dbcContainerConnectionDetailsFactory}.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
class DeprecatedPostgresR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<PostgreSQLContainer<?>, R2dbcConnectionDetails> {

	DeprecatedPostgresR2dbcContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	public R2dbcConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<PostgreSQLContainer<?>> source) {
		return new PostgresR2dbcDatabaseContainerConnectionDetails(source);
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class PostgresR2dbcDatabaseContainerConnectionDetails
			extends ContainerConnectionDetails<PostgreSQLContainer<?>> implements R2dbcConnectionDetails {

		PostgresR2dbcDatabaseContainerConnectionDetails(ContainerConnectionSource<PostgreSQLContainer<?>> source) {
			super(source);
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return PostgreSQLR2DBCDatabaseContainer.getOptions(getContainer());
		}

	}

}
