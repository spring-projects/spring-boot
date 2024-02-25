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

package org.springframework.boot.docker.compose.service.connection.postgres;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.docker.compose.service.connection.jdbc.JdbcUrlBuilder;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for a {@code postgres} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class PostgresJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	private static final String[] POSTGRES_CONTAINER_NAMES = { "postgres", "bitnami/postgresql" };

	/**
	 * Constructs a new instance of the
	 * {@code PostgresJdbcDockerComposeConnectionDetailsFactory} class.
	 * @param postgresContainerNames an array of strings representing the names of the
	 * Postgres containers
	 */
	protected PostgresJdbcDockerComposeConnectionDetailsFactory() {
		super(POSTGRES_CONTAINER_NAMES);
	}

	/**
	 * Returns the JDBC connection details for a Docker Compose connection.
	 * @param source the Docker Compose connection source
	 * @return the JDBC connection details
	 */
	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new PostgresJdbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@code postgres} {@link RunningService}.
	 */
	static class PostgresJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private static final JdbcUrlBuilder jdbcUrlBuilder = new JdbcUrlBuilder("postgresql", 5432);

		private final PostgresEnvironment environment;

		private final String jdbcUrl;

		/**
		 * Constructs a new instance of PostgresJdbcDockerComposeConnectionDetails with
		 * the provided RunningService.
		 * @param service the RunningService object representing the running service
		 */
		PostgresJdbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new PostgresEnvironment(service.env());
			this.jdbcUrl = jdbcUrlBuilder.build(service, this.environment.getDatabase());
		}

		/**
		 * Returns the username associated with the current environment.
		 * @return the username
		 */
		@Override
		public String getUsername() {
			return this.environment.getUsername();
		}

		/**
		 * Returns the password for the Postgres JDBC connection.
		 * @return the password for the Postgres JDBC connection
		 */
		@Override
		public String getPassword() {
			return this.environment.getPassword();
		}

		/**
		 * Returns the JDBC URL for the PostgresJdbcDockerComposeConnectionDetails.
		 * @return the JDBC URL
		 */
		@Override
		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

	}

}
