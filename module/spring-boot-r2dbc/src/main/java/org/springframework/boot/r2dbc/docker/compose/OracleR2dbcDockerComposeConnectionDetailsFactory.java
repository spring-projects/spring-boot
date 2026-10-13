/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.docker.compose;

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;

/**
 * Base class for a {@link DockerComposeConnectionDetailsFactory} to create
 * {@link R2dbcConnectionDetails} for an {@code oracle-free} or {@code oracle-xe} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract class OracleR2dbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<R2dbcConnectionDetails> {

	private final String defaultDatabase;

	OracleR2dbcDockerComposeConnectionDetailsFactory(OracleContainer container) {
		super(container.getImageName(), "io.r2dbc.spi.ConnectionFactoryOptions");
		this.defaultDatabase = container.getDefaultDatabase();
	}

	@Override
	protected R2dbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OracleDbR2dbcDockerComposeConnectionDetails(source.getRunningService(), this.defaultDatabase);
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@code gvenzl/oracle-xe}
	 * {@link RunningService}.
	 */
	static class OracleDbR2dbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements R2dbcConnectionDetails {

		private static final ConnectionFactoryOptionsBuilder connectionFactoryOptionsBuilder = new ConnectionFactoryOptionsBuilder(
				"oracle", 1521);

		private final ConnectionFactoryOptions connectionFactoryOptions;

		OracleDbR2dbcDockerComposeConnectionDetails(RunningService service, String defaultDatabase) {
			super(service);
			OracleEnvironment environment = new OracleEnvironment(service.env(), defaultDatabase);
			this.connectionFactoryOptions = connectionFactoryOptionsBuilder.build(service, environment.getDatabase(),
					environment.getUsername(), environment.getPassword());
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return this.connectionFactoryOptions;
		}

	}

}
