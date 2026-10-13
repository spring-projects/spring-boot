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
 * for a {@code clickhouse} service.
 *
 * @author Stephane Nicoll
 */
class ClickHouseR2dbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<R2dbcConnectionDetails> {

	ClickHouseR2dbcDockerComposeConnectionDetailsFactory() {
		super("clickhouse/clickhouse-server", "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	protected R2dbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ClickhouseDbR2dbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@code clickhouse}
	 * {@link RunningService}.
	 */
	static class ClickhouseDbR2dbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements R2dbcConnectionDetails {

		private static final ConnectionFactoryOptionsBuilder connectionFactoryOptionsBuilder = new ConnectionFactoryOptionsBuilder(
				"clickhouse", 8123);

		private final ConnectionFactoryOptions connectionFactoryOptions;

		ClickhouseDbR2dbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			ClickHouseEnvironment environment = new ClickHouseEnvironment(service.env());
			this.connectionFactoryOptions = connectionFactoryOptionsBuilder.build(service, environment.getDatabase(),
					environment.getUsername(), environment.getPassword());
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return this.connectionFactoryOptions;
		}

	}

}
