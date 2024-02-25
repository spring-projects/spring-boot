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
import org.testcontainers.containers.MSSQLR2DBCDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;

import org.springframework.boot.autoconfigure.r2dbc.R2dbcConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link R2dbcConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link MSSQLServerContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class SqlServerR2dbcContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<MSSQLServerContainer<?>, R2dbcConnectionDetails> {

	/**
     * Constructs a new SqlServerR2dbcContainerConnectionDetailsFactory with the specified connection name and connection factory options.
     *
     * @param connectionName the name of the connection
     * @param connectionFactoryOptions the options for the connection factory
     */
    SqlServerR2dbcContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "io.r2dbc.spi.ConnectionFactoryOptions");
	}

	/**
     * Returns the R2dbcConnectionDetails for the container connection.
     *
     * @param source the ContainerConnectionSource for the MSSQLServerContainer
     * @return the R2dbcConnectionDetails for the container connection
     */
    @Override
	public R2dbcConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<MSSQLServerContainer<?>> source) {
		return new MsSqlServerR2dbcDatabaseContainerConnectionDetails(source);
	}

	/**
	 * {@link R2dbcConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class MsSqlServerR2dbcDatabaseContainerConnectionDetails
			extends ContainerConnectionDetails<MSSQLServerContainer<?>> implements R2dbcConnectionDetails {

		/**
         * Constructs a new MsSqlServerR2dbcDatabaseContainerConnectionDetails object with the provided ContainerConnectionSource.
         * 
         * @param source the ContainerConnectionSource used to create the connection details
         */
        private MsSqlServerR2dbcDatabaseContainerConnectionDetails(
				ContainerConnectionSource<MSSQLServerContainer<?>> source) {
			super(source);
		}

		/**
         * Returns the connection factory options for the MsSqlServerR2dbcDatabaseContainerConnectionDetails.
         * 
         * @return the connection factory options
         */
        @Override
		public ConnectionFactoryOptions getConnectionFactoryOptions() {
			return MSSQLR2DBCDatabaseContainer.getOptions(getContainer());
		}

	}

}
