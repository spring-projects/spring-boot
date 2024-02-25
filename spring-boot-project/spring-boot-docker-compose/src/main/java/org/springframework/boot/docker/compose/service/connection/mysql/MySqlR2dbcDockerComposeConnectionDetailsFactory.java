/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.docker.compose.service.connection.mysql;

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.docker.compose.service.connection.r2dbc.ConnectionFactoryOptionsBuilder;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link R2dbcConnectionDetails}
 * for a {@code mysql} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MySqlR2dbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<R2dbcConnectionDetails> {

	private static final String[] MYSQL_CONTAINER_NAMES = { "mysql", "bitnami/mysql" };

	/**
	 * Constructs a new MySqlR2dbcDockerComposeConnectionDetailsFactory object.
	 * @param containerNames the names of the MySQL containers
	 * @param connectionFactoryOptions the options for the R2DBC connection factory
	 */
	MySqlR2dbcDockerComposeConnectionDetailsFactory() {
		super(MYSQL_CONTAINER_NAMES, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	/**
	 * Returns the R2dbcConnectionDetails for connecting to a MySQL database running in a
	 * Docker Compose environment.
	 * @param source the DockerComposeConnectionSource containing the details of the
	 * running service
	 * @return the R2dbcConnectionDetails for connecting to the MySQL database
	 */
	@Override
	protected R2dbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new MySqlR2dbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@code mysql} {@link RunningService}.
	 */
	static class MySqlR2dbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements R2dbcConnectionDetails {

		private static final ConnectionFactoryOptionsBuilder connectionFactoryOptionsBuilder = new ConnectionFactoryOptionsBuilder(
				"mysql", 3306);

		private final ConnectionFactoryOptions connectionFactoryOptions;

		/**
		 * Constructs a new MySqlR2dbcDockerComposeConnectionDetails object with the given
		 * RunningService.
		 * @param service the RunningService object representing the running service
		 */
		MySqlR2dbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			MySqlEnvironment environment = new MySqlEnvironment(service.env());
			this.connectionFactoryOptions = connectionFactoryOptionsBuilder.build(service, environment.getDatabase(),
					environment.getUsername(), environment.getPassword());
		}

		/**
		 * Returns the connection factory options for the MySQL R2DBC Docker Compose
		 * connection details.
		 * @return the connection factory options
		 */
		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return this.connectionFactoryOptions;
		}

	}

}
