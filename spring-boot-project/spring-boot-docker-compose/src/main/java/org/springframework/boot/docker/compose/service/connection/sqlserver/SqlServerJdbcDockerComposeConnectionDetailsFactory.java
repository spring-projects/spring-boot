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

package org.springframework.boot.docker.compose.service.connection.sqlserver;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.docker.compose.service.connection.jdbc.JdbcUrlBuilder;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for a {@code mssql/server} service.
 *
 * @author Andy Wilkinson
 */
class SqlServerJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	protected SqlServerJdbcDockerComposeConnectionDetailsFactory() {
		super("mssql/server");
	}

	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new SqlServerJdbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@code mssql/server}
	 * {@link RunningService}.
	 */
	static class SqlServerJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private static final JdbcUrlBuilder jdbcUrlBuilder = new JdbcUrlBuilder("sqlserver", 1433);

		private final SqlServerEnvironment environment;

		private final String jdbcUrl;

		SqlServerJdbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new SqlServerEnvironment(service.env());
			this.jdbcUrl = disableEncryptionIfNecessary(jdbcUrlBuilder.build(service, ""));
		}

		private String disableEncryptionIfNecessary(String jdbcUrl) {
			if (jdbcUrl.contains(";encrypt=false;")) {
				return jdbcUrl;
			}
			StringBuilder jdbcUrlBuilder = new StringBuilder(jdbcUrl);
			if (!jdbcUrl.endsWith(";")) {
				jdbcUrlBuilder.append(";");
			}
			jdbcUrlBuilder.append("encrypt=false;");
			return jdbcUrlBuilder.toString();
		}

		@Override
		public String getUsername() {
			return this.environment.getUsername();
		}

		@Override
		public String getPassword() {
			return this.environment.getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return this.jdbcUrl;
		}

	}

}
