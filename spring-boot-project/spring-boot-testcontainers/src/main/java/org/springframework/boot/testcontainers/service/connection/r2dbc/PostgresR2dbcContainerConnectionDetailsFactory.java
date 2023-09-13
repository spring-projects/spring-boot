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

package org.springframework.boot.testcontainers.service.connection.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.PostgreSQLR2DBCDatabaseContainer;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link R2dbcConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link PostgreSQLContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class PostgresR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<PostgreSQLContainer<?>, R2dbcConnectionDetails> {

	PostgresR2dbcContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	@Override
	public R2dbcConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<PostgreSQLContainer<?>> source) {
		return new PostgresR2dbcDatabaseContainerConnectionDetails(source);
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class PostgresR2dbcDatabaseContainerConnectionDetails
			extends ContainerConnectionDetails<PostgreSQLContainer<?>> implements R2dbcConnectionDetails {

		PostgresR2dbcDatabaseContainerConnectionDetails(ContainerConnectionSource<PostgreSQLContainer<?>> source) {
			super(source);
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return PostgreSQLR2DBCDatabaseContainer.getOptions(getContainer());
		}

	}

}
