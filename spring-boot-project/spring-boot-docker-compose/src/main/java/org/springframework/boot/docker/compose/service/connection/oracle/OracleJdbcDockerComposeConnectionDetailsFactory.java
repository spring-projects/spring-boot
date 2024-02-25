/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.oracle;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.util.StringUtils;

/**
 * Base class for a {@link DockerComposeConnectionDetailsFactory} to create
 * {@link JdbcConnectionDetails} for an {@code oracle-free} or {@code oracle-xe} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
abstract class OracleJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	private final String defaultDatabase;

	/**
	 * Constructs a new OracleJdbcDockerComposeConnectionDetailsFactory with the specified
	 * OracleContainer.
	 * @param container the OracleContainer to use for creating the connection details
	 * factory
	 */
	protected OracleJdbcDockerComposeConnectionDetailsFactory(OracleContainer container) {
		super(container.getImageName());
		this.defaultDatabase = container.getDefaultDatabase();
	}

	/**
	 * Returns the JDBC connection details for connecting to a Docker Compose service.
	 * @param source the Docker Compose connection source
	 * @return the JDBC connection details
	 */
	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OracleJdbcDockerComposeConnectionDetails(source.getRunningService(), this.defaultDatabase);
	}

	/**
	 * {@link JdbcConnectionDetails} backed by an {@code oracle-xe} or {@code oracle-free}
	 * {@link RunningService}.
	 */
	static class OracleJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private static final String PARAMETERS_LABEL = "org.springframework.boot.jdbc.parameters";

		private final OracleEnvironment environment;

		private final String jdbcUrl;

		/**
		 * Constructs a new OracleJdbcDockerComposeConnectionDetails object with the
		 * specified RunningService and defaultDatabase.
		 * @param service the RunningService object representing the Oracle database
		 * service
		 * @param defaultDatabase the default database name to connect to
		 */
		OracleJdbcDockerComposeConnectionDetails(RunningService service, String defaultDatabase) {
			super(service);
			this.environment = new OracleEnvironment(service.env(), defaultDatabase);
			this.jdbcUrl = "jdbc:oracle:thin:@" + service.host() + ":" + service.ports().get(1521) + "/"
					+ this.environment.getDatabase() + getParameters(service);
		}

		/**
		 * Returns the parameters of the given RunningService.
		 * @param service the RunningService object to retrieve parameters from
		 * @return a String representing the parameters, or an empty string if no
		 * parameters are found
		 */
		private String getParameters(RunningService service) {
			String parameters = service.labels().get(PARAMETERS_LABEL);
			return (StringUtils.hasLength(parameters)) ? "?" + parameters : "";
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
		 * Returns the password for the Oracle JDBC connection.
		 * @return the password for the Oracle JDBC connection
		 */
		@Override
		public String getPassword() {
			return this.environment.getPassword();
		}

		/**
		 * Returns the JDBC URL for the OracleJdbcDockerComposeConnectionDetails.
		 * @return the JDBC URL
		 */
		@Override
		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

	}

}
