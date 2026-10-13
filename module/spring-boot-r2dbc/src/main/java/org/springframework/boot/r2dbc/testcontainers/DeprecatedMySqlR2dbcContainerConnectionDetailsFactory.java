/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.testcontainers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.MySQLR2DBCDatabaseContainer;

import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link R2dbcConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link MySQLContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
 * {@link MySqlR2dbcContainerConnectionDetailsFactory}.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
class DeprecatedMySqlR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<MySQLContainer<?>, R2dbcConnectionDetails> {

	DeprecatedMySqlR2dbcContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	public R2dbcConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<MySQLContainer<?>> source) {
		return new MySqlR2dbcDatabaseContainerConnectionDetails(source);
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class MySqlR2dbcDatabaseContainerConnectionDetails
			extends ContainerConnectionDetails<MySQLContainer<?>> implements R2dbcConnectionDetails {

		private MySqlR2dbcDatabaseContainerConnectionDetails(ContainerConnectionSource<MySQLContainer<?>> source) {
			super(source);
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return MySQLR2DBCDatabaseContainer.getOptions(getContainer());
		}

	}

}
