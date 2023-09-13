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

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.docker.compose.service.connection.r2dbc.ConnectionFactoryOptionsBuilder;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link R2dbcConnectionDetails}
 * for an {@code oracle-xe} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class OracleR2dbcDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<R2dbcConnectionDetails> {

	OracleR2dbcDockerComposeConnectionDetailsFactory() {
		super("gvenzl/oracle-xe", "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	protected R2dbcConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new OracleDbR2dbcDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@code gvenzl/oracle-xe}
	 * {@link RunningService}.
	 */
	static class OracleDbR2dbcDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements R2dbcConnectionDetails {

		private static final ConnectionFactoryOptionsBuilder connectionFactoryOptionsBuilder = new ConnectionFactoryOptionsBuilder(
				"oracle", 1521);

		private final ConnectionFactoryOptions connectionFactoryOptions;

		OracleDbR2dbcDockerComposeConnectionDetails(RunningService service) {
			super(service);
			OracleEnvironment environment = new OracleEnvironment(service.env());
			this.connectionFactoryOptions = connectionFactoryOptionsBuilder.build(service, environment.getDatabase(),
					environment.getUsername(), environment.getPassword());
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return this.connectionFactoryOptions;
		}

	}

}
