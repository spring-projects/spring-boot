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

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.docker.compose.service.connection.jdbc.JdbcUrlBuilder;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for a {@code mariadb} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class MariaDbJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	private static final String[] MARIADB_CONTAINER_NAMES = { "mariadb", "bitnami/mariadb" };

	/**
     * Creates a new instance of the MariaDbJdbcDockerComposeConnectionDetailsFactory class.
     * This constructor initializes the object with the specified MariaDB container names.
     *
     * @param containerNames an array of strings representing the names of the MariaDB containers
     */
    protected MariaDbJdbcDockerComposeConnectionDetailsFactory() {
		super(MARIADB_CONTAINER_NAMES);
	}

	/**
     * Returns the JDBC connection details for a Docker Compose connection source.
     * 
     * @param source the Docker Compose connection source
     * @return the JDBC connection details
     */
    @Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new MariaDbJdbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@code mariadb} {@link RunningService}.
	 */
	static class MariaDbJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private static final JdbcUrlBuilder jdbcUrlBuilder = new JdbcUrlBuilder("mariadb", 3306);

		private final MariaDbEnvironment environment;

		private final String jdbcUrl;

		/**
         * Constructs a new MariaDbJdbcDockerComposeConnectionDetails object with the provided RunningService.
         * 
         * @param service the RunningService object representing the running service
         */
        MariaDbJdbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new MariaDbEnvironment(service.env());
			this.jdbcUrl = jdbcUrlBuilder.build(service, this.environment.getDatabase());
		}

		/**
         * Returns the username associated with this MariaDbJdbcDockerComposeConnectionDetails.
         *
         * @return the username associated with this MariaDbJdbcDockerComposeConnectionDetails
         */
        @Override
		public String getUsername() {
			return this.environment.getUsername();
		}

		/**
         * Returns the password for the MariaDB JDBC connection.
         * 
         * @return the password for the MariaDB JDBC connection
         */
        @Override
		public String getPassword() {
			return this.environment.getPassword();
		}

		/**
         * Returns the JDBC URL for the MariaDbJdbcDockerComposeConnectionDetails.
         *
         * @return the JDBC URL
         */
        @Override
		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

	}

}
