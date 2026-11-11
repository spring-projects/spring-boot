/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.testcontainers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.clickhouse.ClickHouseR2DBCDatabaseContainer;

import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link R2dbcConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link ClickHouseContainer}.
 *
 * @author Eddú Meléndez
 */
class ClickHouseR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ClickHouseContainer, R2dbcConnectionDetails> {

	ClickHouseR2dbcContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	public R2dbcConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<ClickHouseContainer> source) {
		return new ClickHouseR2dbcDatabaseContainerConnectionDetails(source);
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class ClickHouseR2dbcDatabaseContainerConnectionDetails
			extends ContainerConnectionDetails<ClickHouseContainer> implements R2dbcConnectionDetails {

		private ClickHouseR2dbcDatabaseContainerConnectionDetails(
				ContainerConnectionSource<ClickHouseContainer> source) {
			super(source);
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return ClickHouseR2DBCDatabaseContainer.getOptions(getContainer());
		}

	}

}
