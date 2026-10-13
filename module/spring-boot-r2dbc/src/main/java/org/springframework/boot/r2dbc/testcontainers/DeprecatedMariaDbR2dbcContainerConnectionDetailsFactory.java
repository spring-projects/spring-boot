/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.testcontainers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MariaDBR2DBCDatabaseContainer;

import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link R2dbcConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link MariaDBContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @deprecated since 4.0.0 for removal in 4.2.0 in favor of
 * {@link MariaDbR2dbcContainerConnectionDetailsFactory}.
 */
@Deprecated(since = "4.0.0", forRemoval = true)
class DeprecatedMariaDbR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<MariaDBContainer<?>, R2dbcConnectionDetails> {

	DeprecatedMariaDbR2dbcContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	public R2dbcConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<MariaDBContainer<?>> source) {
		return new MariaDbR2dbcDatabaseContainerConnectionDetails(source);
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class MariaDbR2dbcDatabaseContainerConnectionDetails
			extends ContainerConnectionDetails<MariaDBContainer<?>> implements R2dbcConnectionDetails {

		private MariaDbR2dbcDatabaseContainerConnectionDetails(ContainerConnectionSource<MariaDBContainer<?>> source) {
			super(source);
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return MariaDBR2DBCDatabaseContainer.getOptions(getContainer());
		}

	}

}
