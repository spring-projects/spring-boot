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
 * {@link DockerComposeConnectionDetailsFactory} to create {@link R2dbcConnectionDetails}
 * for a {@code mssql} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class SqlServerR2dbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<R2dbcConnectionDetails> {

	SqlServerR2dbcDockerComposeConnectionDetailsFactory() {
		super("mssql/server", "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	protected R2dbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new SqlServerR2dbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@code mssql} {@link RunningService}.
	 */
	static class SqlServerR2dbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements R2dbcConnectionDetails {

		private static final ConnectionFactoryOptionsBuilder connectionFactoryOptionsBuilder = new ConnectionFactoryOptionsBuilder(
				"mssql", 1433);

		private final ConnectionFactoryOptions connectionFactoryOptions;

		SqlServerR2dbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			SqlServerEnvironment environment = new SqlServerEnvironment(service.env());
			this.connectionFactoryOptions = connectionFactoryOptionsBuilder.build(service, "",
					environment.getUsername(), environment.getPassword());
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return this.connectionFactoryOptions;
		}

	}

}
