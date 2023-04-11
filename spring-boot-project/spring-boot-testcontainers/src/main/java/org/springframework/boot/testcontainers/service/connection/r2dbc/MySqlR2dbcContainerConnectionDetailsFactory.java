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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.MySQLR2DBCDatabaseContainer;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

/**
 * {@link ConnectionDetailsFactory} for
 * {@link R2dbcServiceConnection @R2dbcServiceConnection}- annotated
 * {@link MySQLContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class MySqlR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<R2dbcServiceConnection, R2dbcConnectionDetails, MySQLContainer<?>> {

	@Override
	public R2dbcConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<R2dbcServiceConnection, R2dbcConnectionDetails, MySQLContainer<?>> source) {
		return new R2dbcDatabaseContainerConnectionDetails(source.getContainer());
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class R2dbcDatabaseContainerConnectionDetails implements R2dbcConnectionDetails {

		private final MySQLContainer<?> container;

		private R2dbcDatabaseContainerConnectionDetails(MySQLContainer<?> container) {
			this.container = container;
		}

		@Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return MySQLR2DBCDatabaseContainer.getOptions(this.container);
		}

	}

}
