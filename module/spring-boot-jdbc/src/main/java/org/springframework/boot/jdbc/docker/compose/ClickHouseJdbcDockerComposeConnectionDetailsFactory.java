/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.docker.compose;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link JdbcConnectionDetails}
 * for a {@code clickhouse} service.
 *
 * @author Stephane Nicoll
 */
class ClickHouseJdbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<JdbcConnectionDetails> {

	protected ClickHouseJdbcDockerComposeConnectionDetailsFactory() {
		super("clickhouse/clickhouse-server");
	}

	@Override
	protected JdbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ClickhouseJdbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link JdbcConnectionDetails} backed by a {@code clickhouse}
	 * {@link RunningService}.
	 */
	static class ClickhouseJdbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements JdbcConnectionDetails {

		private static final JdbcUrlBuilder jdbcUrlBuilder = new JdbcUrlBuilder("clickhouse", 8123);

		private final ClickHouseEnvironment environment;

		private final String jdbcUrl;

		ClickhouseJdbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ClickHouseEnvironment(service.env());
			this.jdbcUrl = jdbcUrlBuilder.build(service, this.environment.getDatabase());
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
