/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.testcontainers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.oracle.OracleR2DBCDatabaseContainer;

import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link R2dbcConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link OracleContainer}.
 *
 * @author Eddú Meléndez
 */
class OracleFreeR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<OracleContainer, R2dbcConnectionDetails> {

	OracleFreeR2dbcContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	public R2dbcConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<OracleContainer> source) {
		return new R2dbcDatabaseContainerConnectionDetails(source);
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class R2dbcDatabaseContainerConnectionDetails
			extends ContainerConnectionDetails<OracleContainer> implements R2dbcConnectionDetails {

		private R2dbcDatabaseContainerConnectionDetails(ContainerConnectionSource<OracleContainer> source) {
			super(source);
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return OracleR2DBCDatabaseContainer.getOptions(getContainer());
		}

	}

}
