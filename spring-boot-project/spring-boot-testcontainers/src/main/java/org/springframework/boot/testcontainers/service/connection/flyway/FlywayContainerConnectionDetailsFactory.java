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

package org.springframework.boot.testcontainers.service.connection.flyway;

import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.boot.autoconfigure.flyway.FlywayConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link FlywayConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link JdbcDatabaseContainer}.
 *
 * @author Andy Wilkinson
 */
class FlywayContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<JdbcDatabaseContainer<?>, FlywayConnectionDetails> {

	/**
     * Retrieves the connection details for the specified container connection source.
     *
     * @param source the container connection source
     * @return the flyway connection details for the specified container connection source
     */
    @Override
	protected FlywayConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
		return new FlywayContainerConnectionDetails(source);
	}

	/**
	 * {@link FlywayConnectionDetails} backed by a {@link JdbcDatabaseContainer}.
	 */
	private static final class FlywayContainerConnectionDetails
			extends ContainerConnectionDetails<JdbcDatabaseContainer<?>> implements FlywayConnectionDetails {

		/**
         * Constructs a new FlywayContainerConnectionDetails object with the specified ContainerConnectionSource.
         *
         * @param source the ContainerConnectionSource used to create the FlywayContainerConnectionDetails object
         */
        private FlywayContainerConnectionDetails(ContainerConnectionSource<JdbcDatabaseContainer<?>> source) {
			super(source);
		}

		/**
         * Returns the username of the container associated with this connection details.
         * 
         * @return the username of the container
         */
        @Override
		public String getUsername() {
			return getContainer().getUsername();
		}

		/**
         * Returns the password for the Flyway container connection details.
         * 
         * @return the password for the Flyway container connection details
         */
        @Override
		public String getPassword() {
			return getContainer().getPassword();
		}

		/**
         * Returns the JDBC URL for the Flyway container connection.
         * 
         * @return the JDBC URL for the Flyway container connection
         */
        @Override
		public String getJdbcUrl() {
			return getContainer().getJdbcUrl();
		}

		/**
         * Returns the driver class name used by the Flyway container connection details.
         * 
         * @return the driver class name
         */
        @Override
		public String getDriverClassName() {
			return getContainer().getDriverClassName();
		}

	}

}
