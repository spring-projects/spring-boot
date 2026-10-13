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
 * for a {@code mariadb} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MariaDbR2dbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<R2dbcConnectionDetails> {

	MariaDbR2dbcDockerComposeConnectionDetailsFactory() {
		super("mariadb", "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	protected R2dbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new MariaDbR2dbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@code mariadb} {@link RunningService}.
	 */
	static class MariaDbR2dbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements R2dbcConnectionDetails {

		private static final ConnectionFactoryOptionsBuilder connectionFactoryOptionsBuilder = new ConnectionFactoryOptionsBuilder(
				"mariadb", 3306);

		private final ConnectionFactoryOptions connectionFactoryOptions;

		MariaDbR2dbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			MariaDbEnvironment environment = new MariaDbEnvironment(service.env());
			this.connectionFactoryOptions = connectionFactoryOptionsBuilder.build(service, environment.getDatabase(),
					environment.getUsername(), environment.getPassword());
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return this.connectionFactoryOptions;
		}

	}

}
