/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.r2dbc.docker.compose;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcConnectionDetails;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link R2dbcConnectionDetails}
 * for a {@code postgres} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class PostgresR2dbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<R2dbcConnectionDetails> {

	PostgresR2dbcDockerComposeConnectionDetailsFactory() {
		super("postgres", "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	protected R2dbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new PostgresDbR2dbcDockerComposeConnectionDetails(source.getRunningService(), source.getEnvironment());
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@code postgres} {@link RunningService}.
	 */
	static class PostgresDbR2dbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements R2dbcConnectionDetails {

		private static final Option<String> APPLICATION_NAME = Option.valueOf("applicationName");

		private static final ConnectionFactoryOptionsBuilder connectionFactoryOptionsBuilder = new ConnectionFactoryOptionsBuilder(
				"postgresql", 5432);

		private final ConnectionFactoryOptions connectionFactoryOptions;

		PostgresDbR2dbcDockerComposeConnectionDetails(RunningService service, Environment environment) {
			super(service);
			this.connectionFactoryOptions = getConnectionFactoryOptions(service, environment);
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return this.connectionFactoryOptions;
		}

		private static ConnectionFactoryOptions getConnectionFactoryOptions(RunningService service,
				Environment environment) {
			PostgresEnvironment env = new PostgresEnvironment(service.env());
			ConnectionFactoryOptions connectionFactoryOptions = connectionFactoryOptionsBuilder.build(service,
					env.getDatabase(), env.getUsername(), env.getPassword());
			return addApplicationNameIfNecessary(connectionFactoryOptions, environment);
		}

		private static ConnectionFactoryOptions addApplicationNameIfNecessary(
				ConnectionFactoryOptions connectionFactoryOptions, Environment environment) {
			if (connectionFactoryOptions.hasOption(APPLICATION_NAME)) {
				return connectionFactoryOptions;
			}
			String applicationName = environment.getProperty("spring.application.name");
			if (!StringUtils.hasText(applicationName)) {
				return connectionFactoryOptions;
			}
			return connectionFactoryOptions.mutate().option(APPLICATION_NAME, applicationName).build();
		}

	}

}
