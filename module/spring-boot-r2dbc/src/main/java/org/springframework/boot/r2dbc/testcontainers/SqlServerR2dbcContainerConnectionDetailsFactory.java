/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.testcontainers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.mssqlserver.MSSQLR2DBCDatabaseContainer;
import org.testcontainers.mssqlserver.MSSQLServerContainer;

import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link R2dbcConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link MSSQLServerContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class SqlServerR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<MSSQLServerContainer, R2dbcConnectionDetails> {

	SqlServerR2dbcContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	public R2dbcConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<MSSQLServerContainer> source) {
		return new MsSqlServerR2dbcDatabaseContainerConnectionDetails(source);
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class MsSqlServerR2dbcDatabaseContainerConnectionDetails
			extends ContainerConnectionDetails<MSSQLServerContainer> implements R2dbcConnectionDetails {

		private MsSqlServerR2dbcDatabaseContainerConnectionDetails(
				ContainerConnectionSource<MSSQLServerContainer> source) {
			super(source);
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return MSSQLR2DBCDatabaseContainer.getOptions(getContainer());
		}

	}

}
