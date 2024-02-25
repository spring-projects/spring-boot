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

package org.springframework.boot.docker.compose.service.connection.mariadb;

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.docker.compose.service.connection.r2dbc.ConnectionFactoryOptionsBuilder;

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

	private static final String[] MARIADB_CONTAINER_NAMES = { "mariadb", "bitnami/mariadb" };

	/**
     * Constructs a new MariaDbR2dbcDockerComposeConnectionDetailsFactory.
     * 
     * @param containerNames the names of the MariaDB containers
     * @param connectionFactoryOptions the options for the connection factory
     */
    MariaDbR2dbcDockerComposeConnectionDetailsFactory() {
		super(MARIADB_CONTAINER_NAMES, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	/**
     * Retrieves the connection details for a Docker Compose service.
     * 
     * @param source the Docker Compose connection source
     * @return the R2dbcConnectionDetails for the specified Docker Compose service
     */
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

		/**
         * Constructs a new MariaDbR2dbcDockerComposeConnectionDetails object with the provided RunningService.
         * 
         * @param service the RunningService object representing the running MariaDB service
         */
        MariaDbR2dbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			MariaDbEnvironment environment = new MariaDbEnvironment(service.env());
			this.connectionFactoryOptions = connectionFactoryOptionsBuilder.build(service, environment.getDatabase(),
					environment.getUsername(), environment.getPassword());
		}

		/**
         * Returns the connection factory options for establishing a connection.
         *
         * @return the connection factory options
         */
        @Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return this.connectionFactoryOptions;
		}

	}

}
